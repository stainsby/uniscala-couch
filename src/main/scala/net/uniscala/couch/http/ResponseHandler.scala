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


class ResponseHandler(
  promise: Promise[Response]
) extends ChannelInboundMessageHandlerAdapter[HttpResponse] {
  
  @volatile private var response: Response = null
  
  class ChunkDownloader extends ChannelInboundMessageHandlerAdapter[HttpContent] {
    
    override def messageReceived(
      ctx: ChannelHandlerContext,
      chunk: HttpContent
    ) = {
      val buf = chunk.content
      try {
        onContent(ctx, buf.nioBuffer)
      } catch {
        case err: Throwable => {
          onContentFailure(ctx, err)
        }
      }
      chunk match {
        case _: LastHttpContent => {
          onContentEnd(ctx)
          ctx.channel.close
        }
        case _ => { }
      }
    }
  }
  
  lazy val chunkDownloader = new this.ChunkDownloader
  
  protected def onContent(ctx: ChannelHandlerContext, buf: ByteBuffer) = {
    assert(response != null, "illegal response state")
    response.appendContent(buf)
  }
  
  protected def onContentEnd(ctx: ChannelHandlerContext) = {
    
    assert(this.response != null, "illegal response state")
    
    try {
      this.response.contentPipe.sink.close
    }
    
    try {
      ctx.channel.close
    }
  }
  
  protected def onContentFailure(ctx: ChannelHandlerContext, err: Throwable): Unit =
    this.response.content.failure(err)
  
  override def messageReceived(
    ctx: ChannelHandlerContext,
    msg: HttpResponse
  ): Unit = {
    assert(this.response == null, "received multiple HTTP responses")
    this.response = new Response(this, msg)
    try {
      msg match {
        case resp: FullHttpResponse => {
          val buf = resp.content
          try {
            onContent(ctx, buf.nioBuffer)
            onContentEnd(ctx)
          } catch {
            case err: Throwable => {
              onContentFailure(ctx, err)
            }
          }
        }
        case resp: HttpResponse => { }
      }
    } finally {
      promise.success(this.response)
    }
  }
  
  override def handlerAdded(ctx: ChannelHandlerContext) = {
    ctx.pipeline.addLast(this.chunkDownloader)
  }
  
  //override def beforeRemove(ctx: ChannelHandlerContext) = {
  //  ctx.pipeline.remove(this.chunkDownloader)
  //}
}
