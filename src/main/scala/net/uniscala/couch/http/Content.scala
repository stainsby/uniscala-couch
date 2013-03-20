package net.uniscala.couch.http

import scala.concurrent.{future, ExecutionContext, Future, Promise}
import scala.io.Source

import java.io.{ByteArrayOutputStream, InputStream, Reader}

import java.nio.channels.{Channels, Pipe}


class Content private[http] (private val _source: Pipe.SourceChannel) {
  
  import ExecutionContext.Implicits.global
  
  @volatile var promise: Promise[_ <: Any] = null // set only once (latch)
  
  def close: Unit = _source.close
  
  private def createStream: InputStream = Channels.newInputStream(_source)
  
  private def createReader: Reader = Channels.newReader(_source, "utf8")
  
  // we need to use a Promise so that ResponseHandler can force failures on us
  // by calling the 'failure' method
  
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
    prom.tryCompleteWith { future { f(createStream) } }
    prom.future
  }
  
  def withReader[T](f: Reader => T): Future[T] = {
    val prom = createPromise[T]
    prom.tryCompleteWith { future { f(createReader) } }
    prom.future
  }
  
  def withSource[T](f: scala.io.Source => T): Future[T] = {
    val prom = createPromise[T]
    prom.tryCompleteWith {
      future { f(Source.fromInputStream((createStream))) }
    }
    prom.future
  }
  
  def bytes: Future[Array[Byte]] = {
    val prom = createPromise[Array[Byte]]
    prom.tryCompleteWith {
      future {
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
        val res = out.toByteArray
        try {
          _source.close
        }
        res
      }
    }
    prom.future
  }
  
  def text: Future[String] = bytes.map(new String(_, "utf8"))
}
