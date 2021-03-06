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

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.Source

import java.io.{InputStream, Reader}
import java.net.InetSocketAddress
import java.util.concurrent.CancellationException

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http._

import net.uniscala.couch.util.Http.Header.CONTENT_LENGTH


object Client {
  
  private val COPY = new HttpMethod("COPY")
  
  def apply(host: String = "localhost", port: Int = 80) =
    new Client(new InetSocketAddress(host, port))
  
  def newRequest(
    path: String,
    method: HttpMethod = HttpMethod.GET,
    contentLengthOpt: Option[Long] = None
  ): FullHttpRequest = {
    assert(path != null, "null path")
    assert(method != null, "null method")
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path)
    contentLengthOpt foreach { len: Long => 
      req.headers.add(CONTENT_LENGTH, len)
    }
    req
  }
  
  def prepareGet(path: String): FullHttpRequest =
    newRequest(path, HttpMethod.GET)
  
  def preparePost(path: String): FullHttpRequest =
    newRequest(path, HttpMethod.POST)
  
  def preparePut(path: String): FullHttpRequest =
    newRequest(path, HttpMethod.PUT)
  
  def prepareDelete(path: String): FullHttpRequest =
    newRequest(path, HttpMethod.DELETE)
  
  def prepareHead(path: String): FullHttpRequest =
    newRequest(path, HttpMethod.HEAD)
  
  def prepareCopy(path: String): FullHttpRequest =
    newRequest(path, COPY)
}


/**
 * A simple HTTP client using Netty.
 */
class Client(val address: InetSocketAddress) {
  
  // TODO: is there a special context for Netty and how can we avoid doing
  // this everywhere?
  import ExecutionContext.Implicits.global
  import Client._
  
  protected lazy val eventGroup = new NioEventLoopGroup
  
  protected lazy val bootstrap: Bootstrap = {
    new Bootstrap().
    group(eventGroup).
    channel(classOf[NioSocketChannel]).
    option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true).
    option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, false).
    handler(createInitializer).
    remoteAddress(address)
  }
  
  protected def createInitializer = new ClientInitializer()
  
  protected def newChannel: Channel = bootstrap.connect.sync.channel
  
  protected def newChannelListener(
    responsePromise: Promise[Response]
  ): ChannelFutureListener = {
    new ChannelFutureListener() {
      override def operationComplete(f: ChannelFuture) = {
        if (!f.isSuccess) {
          responsePromise.failure(f.cause)
        }
      }
    }
  }
  
  /**
   * Send a simple (non-streaming) HTTP request.
   */
  def send(request: FullHttpRequest): Future[Response] = {
    assert(request != null, "null request")
    val responsePromise = Promise[Response]()
    val chan = newChannel
    chan.pipeline.addLast(
      "http-response",
      new ResponseHandler(responsePromise)
    )
    val channelFuture = chan.write(request)
    channelFuture.addListener(newChannelListener(responsePromise))
    chan.flush()
    responsePromise.future
  }
  
  /**
   * Shuts down this object as an HTTP client. No further HTTP operations
   * should be attempted on this, or any derived CouchPath instances, after
   * calling this method.
   */
  def shutdown(): Unit = eventGroup.shutdownGracefully()
  
  
  // convenience methods based on 'send' with content handlers
  
  
  /**
   * Send a simple HTTP request and convert the response to text.
   */
  def text(request: FullHttpRequest): Future[String] =
    send(request).flatMap(_.text)
  
  /**
   * Send a simple HTTP request and convert the response to bytes.
   */
  def bytes(request: FullHttpRequest): Future[Array[Byte]] =
    send(request).flatMap(_.bytes)
  
  /**
   * Send a simple HTTP request and process the response with the the
   * supplied handler.
   */
  def readText[T](request: FullHttpRequest, handler: Reader => T): Future[T] =
    send(request).flatMap(_.readText(handler))
  
  /**
   * Send a simple HTTP request and process the response with the the
   * supplied handler.
   */
  def readSource[T](request: FullHttpRequest, handler: Source => T): Future[T] =
    send(request).flatMap(_.readSource(handler))
  
  /**
   * Send a simple HTTP request and process the response with the the
   * supplied handler.
   */
  def readStream[T](request: FullHttpRequest, handler: InputStream => T): Future[T] =
    send(request).flatMap(_.readStream(handler))
}