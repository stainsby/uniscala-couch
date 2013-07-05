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
  ChannelInboundHandlerAdapter, ChannelHandlerContext, MessageList
}
import io.netty.handler.codec.http._


class ResponseHandler(
  val promise: Promise[Response]
) extends ChannelInboundHandlerAdapter {
  
  @volatile private var response: Response = null
  
  protected def onContent(ctx: ChannelHandlerContext, buf: ByteBuffer) = {
    assertProtocol(response != null, "illegal response state")
    response.appendContent(buf)
  }
  
  protected def onContentEnd(ctx: ChannelHandlerContext) = {
    assertProtocol(this.response != null, "illegal response state")
    promise.success(this.response)
    try { this.response.contentPipe.sink.close }
    try { ctx.channel.close }
  }
  
  protected def onContentFailure(
    ctx: ChannelHandlerContext,
    err: Throwable
   ): Unit = {
    this.promise.failure(err)
    this.response.content.failure(err)
  }
  
  override def messageReceived(
    ctx: ChannelHandlerContext,
    msgs: MessageList[java.lang.Object]
  ): Unit = {
    import scala.collection.JavaConversions.iterableAsScalaIterable
    try {
      msgs.foreach { msg =>
        msg match {
          case resp: HttpResponse => {
            assertProtocol(this.response == null, "multiple responses")
            this.response = new Response(this, resp)
            resp match {
              case _: FullHttpResponse => onContentEnd(ctx)
              case _ => { }
            }
          }
          case chunk: HttpContent => {
            onContent(ctx, chunk.content.nioBuffer)
            chunk match {
              case _: LastHttpContent => onContentEnd(ctx)
              case _ => { }
            }
          }
          case other => {
            assertProtocol(false, "unhandled message: " + other.getClass)
          }
        }
      }
    } catch {
      case err: Throwable => {
        err.printStackTrace // TODO
        onContentFailure(ctx, err)
      }
    } finally {
      msgs.releaseAllAndRecycle
    }
  }
  
  protected def assertProtocol(predicate: Boolean, msg: String) {
    if (!predicate) throw new ProtocolException(msg)
  }
}
