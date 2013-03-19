package net.uniscala.couch.http

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.Source

import java.io.{ByteArrayOutputStream, InputStream, Reader}

import java.nio.channels.{Channels, Pipe}


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
  
  // TODO: put futures inside promises using Promise#tryCompleteWith?
  
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
      try {
        _source.close
      }
    } catch {
      case err: Throwable => prom.failure(err)
    }
    prom.future
  }
  
  def text: Future[String] = bytes.map(new String(_, "utf8"))
}
