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
  
  class ChunkHandler extends ChannelInboundMessageHandlerAdapter[HttpContent] {
    
    override def messageReceived(
      ctx: ChannelHandlerContext,
      chunk: HttpContent
    ) = {
      //println("got chunk")
      val buf = chunk.data
      try {
        onContent(ctx, buf.nioBuffer)
      } catch {
        case err: Throwable => {
          onContentFailure(ctx, err)
        }
      }
      chunk match {
        case _: LastHttpContent => {
          //println("  - LAST chunk")
          onContentEnd(ctx)
          ctx.channel.close
        }
        case _ => {
          //println("  - WASN'T last chunk")
          
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
    val b = promise.success(this.response)
    msg match {
      case resp: FullHttpResponse => {
        //println("FULL HTTP response")
        val buf = resp.data
        try {
          onContent(ctx, buf.nioBuffer)
          onContentEnd(ctx)
        } catch {
          case err: Throwable => {
            onContentFailure(ctx, err)
          }
        }
      }
      case resp: HttpResponse => {
        //println("(expecting chunked content)")
      }
    }
  }
  
  override def afterAdd(ctx: ChannelHandlerContext) = {
    ctx.pipeline.addLast(this.chunkHandler)
  }
  
  override def beforeRemove(ctx: ChannelHandlerContext) = {
    ctx.pipeline.remove(this.chunkHandler)
  }
}
