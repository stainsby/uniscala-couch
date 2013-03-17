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

import java.io.{ByteArrayOutputStream, InputStream, Reader}

import java.nio.ByteBuffer
import java.nio.channels.{Channels, Pipe}

import io.netty.handler.codec.http._


class Response private[http] (
  private val handler: ResponseHandler,
  private val nettyResponse: HttpResponse
) {
  
  import scala.collection.JavaConversions._
  
  class Content private[http] (private val _source: Pipe.SourceChannel) {
    
    import ExecutionContext.Implicits.global
    
    @volatile var promise: Promise[_ <: Any] = null // set only once (latch)
    
    def close: Unit = _source.close
    
    private def createStream: InputStream = Channels.newInputStream(_source)
    
    private def createReader: Reader = Channels.newReader(_source, "utf8")
    
    private def createPromise[T]: Promise[T] = synchronized {
      if (this.promise == null) {
        val prom = Promise[T]()
        this.promise = prom
        prom
      } else {
        throw new IllegalStateException("already reading content")
      }
    }
    
    def failure(err: Throwable): Unit = synchronized {
      val prom = this.promise
      if (prom != null) prom.tryFailure(err)
    }
    
    def withStream[T](f: InputStream => T): Future[T] = {
      val prom = createPromise[T]
      try {
        prom.success(f(createStream))
      } catch {
        case err: Throwable => this.promise.failure(err)
      }
      prom.future
    }
    
    def withReader[T](f: Reader => T): Future[T] = {
      val prom = createPromise[T]
      try {
        prom.success(f(createReader))
      } catch {
        case err: Throwable => prom.failure(err)
      }
      prom.future
    }
    
    def withSource[T](f: scala.io.Source => T): Future[T] = {
      val prom = createPromise[T]
      try {
        prom.success(f(Source.fromInputStream((createStream))))
      } catch {
        case err: Throwable => prom.failure(err)
      }
      prom.future
    }
    
    def bytes: Future[Array[Byte]] = {
      val prom = createPromise[Array[Byte]]
      try {
        val src = _source
        val out = new ByteArrayOutputStream(64*1024)
        val dest = Channels.newChannel(out)
        val buffer = java.nio.ByteBuffer.allocateDirect(8*1024);
        
        while (src.read(buffer) != -1) {
          buffer.flip();
          dest.write(buffer);
          buffer.compact();
        }
        buffer.flip()
        while (buffer.hasRemaining()) {
          dest.write(buffer);
        }
        out.close
        prom.success(out.toByteArray)
      } catch {
        case err: Throwable => prom.failure(err)
      }
      prom.future
    }
    
    def text: Future[String] = bytes.map(new String(_, "utf8"))
  }
  
  
  private[http] lazy val contentPipe: Pipe = Pipe.open()
  
  lazy val statusCode: Int = nettyResponse.getStatus.code
  
  lazy val statusMessageOption: Option[String] =
    Option(nettyResponse.getStatus.reasonPhrase)
  
  lazy val content = new this.Content(contentPipe.source)
  
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
