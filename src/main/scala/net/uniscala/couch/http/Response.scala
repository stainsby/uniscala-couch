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

import scala.concurrent.Future
import scala.io.Source

import java.io.{InputStream, Reader}

import java.nio.ByteBuffer
import java.nio.channels.Pipe

import io.netty.handler.codec.http._


class Response private[http] (
  private val handler: ResponseHandler,
  private val nettyResponse: HttpResponse
) {
  
  import scala.collection.JavaConversions._
  
  
  private[http] lazy val contentPipe: Pipe = Pipe.open()
  
  lazy val statusCode: Int = nettyResponse.getStatus.code
  
  lazy val statusMessageOption: Option[String] =
    Option(nettyResponse.getStatus.reasonPhrase)
  
  lazy val content = new Content(contentPipe.source)
  
  private[http] def appendContent(buf: ByteBuffer): Unit =
    contentPipe.sink.write(buf)
  
  def header(key: String): String = nettyResponse.headers.get(key)
  
  def headers(key: String): Seq[String] = nettyResponse.headers.getAll(key)
  
  def headers: Seq[(String, String)] = {
    nettyResponse.headers.entries map { entry =>
      (entry.getKey, entry.getValue)
    }
  }
  
  
  // convenience methods for accessing content
  
  
  def text(): Future[String] =
    content.text
  
  def bytes(): Future[Array[Byte]] =
    content.bytes
  
  def readText[T](handler: Reader => T): Future[T] =
    content.withReader(handler)
  
  def readSource[T](handler: Source => T): Future[T] =
    content.withSource(handler)
  
  def readStream[T](handler: InputStream => T): Future[T] =
    content.withStream(handler)
}
