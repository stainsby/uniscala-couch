/**
 * This file is part of the Uniscala Couch project.
 * Copyright (C) 2012 Sustainable Software Pty Ltd.
 * This is open source software, licensed under the Apache License
 * version 2.0 license - please see the LICENSE file included in
 * the distribution.
 *
 * Authors:
 * Sam Stainsby (sam@sustainablesoftware.com.au)
 */
package net.uniscala.couch.http

import scala.concurrent.Promise

import java.nio.ByteBuffer

import io.netty.channel.{
  ChannelInboundMessageHandlerAdapter, ChannelHandlerContext
}
import io.netty.handler.codec.http._
import io.netty.logging.InternalLoggerFactory


object ResponseHandler {
  lazy val logger =
    InternalLoggerFactory.getInstance(classOf[ResponseHandler].getSimpleName)
}


class ResponseHandler(
  promise: Promise[Response]
) extends ChannelInboundMessageHandlerAdapter[HttpResponse](
  classOf[HttpResponse]
) {
  
  import ResponseHandler.logger
  
  @volatile private var response: Response = null
  
  class ChunkHandler
  extends ChannelInboundMessageHandlerAdapter[HttpChunk](classOf[HttpChunk]) {
    override def messageReceived(
      ctx: ChannelHandlerContext,
      chunk: HttpChunk
    ) = {
      if (chunk.isLast) {
        onContentEnd(ctx)
        ctx.channel.close
      } else {
        val buf = chunk.getContent
        assert(buf.hasNioBuffer, "expected NIO buffer")
        try {
          onContent(ctx, buf.nioBuffer)
        } catch {
          case err: Throwable => {
            onContentFailure(ctx, err)
          }
        }
      }
    }
  }
  
  lazy val chunkHandler = new this.ChunkHandler
  
  protected def onContent(ctx: ChannelHandlerContext, buf: ByteBuffer) = {
    assert(response != null, "illegal response state")
    response.appendContent(buf)
  }
  
  protected def onContentEnd(ctx: ChannelHandlerContext) = {
    
    assert(response != null, "illegal response state")
    
    val pipe = response.contentPipe
    
    try {
      logger.debug("closing content pipe")
      pipe.sink.close
      logger.debug("content pipe closed OK")
    } catch {
      case err: Throwable =>
        logger.warn("attempt to close response stream failed: " + err)
    }
    
    try {
      logger.debug("closing channel: " + ctx.channel)
      ctx.channel.close
      logger.debug("channel closed OK")
    } catch {
      case err: Throwable =>
        logger.warn("attempt to close channel failed: " + err)
    }
  }
  
  protected def onContentFailure(ctx: ChannelHandlerContext, err: Throwable): Unit =
    response.content.failure(err)
  
  override def messageReceived(
    ctx: ChannelHandlerContext,
    httpResponse: HttpResponse
  ): Unit = {
    import HttpTransferEncoding._
    this.response = new Response(this, httpResponse)
    val b = promise.success(response)
    httpResponse.getTransferEncoding() match {
      case SINGLE => {
        val buf = httpResponse.getContent()
        assert(buf.hasNioBuffer, "invalid content buffer type")
        try {
          onContent(ctx, buf.nioBuffer)
          onContentEnd(ctx)
        } catch {
          case err: Throwable => {
            onContentFailure(ctx, err)
          }
        }
      }
      case STREAMED| CHUNKED => {
        ctx.pipeline.addLast("http-content", chunkHandler)
      }
    }
  }
}
