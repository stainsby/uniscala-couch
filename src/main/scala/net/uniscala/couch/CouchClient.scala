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
package net.uniscala.couch

import scala.concurrent.{ExecutionContext, Future, Promise}

import java.io.{File, InputStream}
import java.net.InetSocketAddress
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.CancellationException

import io.netty.channel.{ChannelFuture, ChannelFutureListener}
import io.netty.handler.codec.http.{FullHttpRequest, HttpMethod, HttpRequest}
import io.netty.handler.stream._

import net.uniscala.json._
import Json._

import http._
import util.{Http, HttpUrl, Mime, Url}

import Http.Header.CONTENT_LENGTH


private[couch] object CouchClient {
  
  private[couch] object Path {
    val ACTIVE_TASKS = "_active_tasks"
    val ALL_DATABASES = "_all_dbs"
    val INFO = "_info"
    val LOG = "_log"
    val REPLICATE = "_replicate"
    val RESTART = "_restart"
    val STATS = "_stats" 
    val UUIDS = "_uuids"
  }
  
  private[couch] object Param {
    val BYTES = "bytes"
    val CANCEL = "cancel"
    val CONTINUOUS = "continuous"
    val COUNT = "count"
    val CREATE_TARGET = "create_target"
    val DOC_IDS = "doc_ids"
    val FILTER = "filter"
    val OFFSET = "offset"
    val PROXY = "proxy"
    val SOURCE = "source"
    val TARGET = "target"
  }
}


/**
 * An HTTP client for communicating with a couch database, and also a
 * 'path' for the root URL (see CouchPath). HTTP Basic authentication 
 * is supported by specifying a BasicCredentials instance for the 
 * credentials option. API methods here represent server-level
 * operations.
 */
class CouchClient(
  val host: String = "localhost",
  val port: Int = 5984,
  credentialsOption: Option[Credentials] = None
) extends Client(new InetSocketAddress(host, port)) with CouchPath {
  
  def this(
    host: String,
    port: Int,
    credentials: Credentials
  ) = this(host, port, Some(credentials))
  
  import ExecutionContext.Implicits.global
  import Couch._
  import CouchClient._
  
  override val parentOption = None
  
  override val client = this
  
  override lazy val baseUrl = Url(host, port=port)
  
  
  /**
   * Sets up the headers for authentication to the database.
   * CUrrently, only basic auth is supported.
   */
  protected def configureAuth(req: HttpRequest): Unit = {
    import Http.Header._
    credentialsOption match {
      case None => { } // do nothing
      case Some(basic: BasicCredentials) => {
        req.headers.set(AUTHORIZATION, basic.authorizationHeader)
      }
    }
  }
  
  /**
   * Sets up the default headers for a request to the database.
   * The 'configureAuth' method is also applied here.
   */
  protected def addDefaultHeaders(req: HttpRequest): Unit = {
    import Http.Header._
    import Mime._
    req.headers.set(USER_AGENT, classOf[CouchClient].getName + " 1.0")
    req.headers.set(HOST, host + ":" + port)
    req.headers.set(ACCEPT, ANY)
    req.headers.set(CONTENT_TYPE, JSON)
    configureAuth(req)
  }
  
  /**
   * Sends a non-streaming request. The 'addDefaultHeaders' method is
   * applied to the request first.
   */
  override def send(request: FullHttpRequest) = {
    addDefaultHeaders(request)
    super.send(request)
  }
  
  private def upload0(
    request: HttpRequest,
    stream: ChunkedByteInput,
    contentType: Mime
  ): Future[Response] = {
    
    import Http.Header._
    
    request.setMethod(HttpMethod.PUT)
    addDefaultHeaders(request)
    request.headers.set(CONTENT_TYPE, contentType.toString)
    val responsePromise = Promise[Response]()
  
    val fut = responsePromise.future
    val chan = newChannel
    val uploader = new ChunkedWriteHandler()
    chan.pipeline.addLast("uploader", uploader)
    chan.pipeline.addLast("responder", new ResponseHandler(responsePromise))
    chan.write(request).addListener(new ChannelFutureListener() {
      override def operationComplete(f: ChannelFuture) = {
        if (!f.isSuccess) {
          responsePromise.failure(f.cause)
        }
      }
    })
    chan.write(stream).addListener(new ChannelFutureListener() {
      override def operationComplete(f: ChannelFuture) = {
        if (!f.isSuccess) {
          responsePromise.failure(f.cause)
        }
        try {
          stream.close
        }
      } 
    })
    chan.write()
    fut
  }
  
  /**
   * Uploads a file using a streaming PUT operation.
   */
  def upload(
    request: HttpRequest,
    file: File,
    contentType: Mime
  ): Future[Response] = {
    assert(request != null, "null request")
    assert(file != null, "null file")
    assert(file.exists, "file doesn't exist")
    request.headers.set(CONTENT_LENGTH, file.length.toString)
    upload0(request, new ChunkedNioFile(file), contentType)
  }
  
  /**
   * Uploads from an input stream using a streaming PUT operation.
   */
  def upload(
    request: HttpRequest,
    in: InputStream,
    contentType: Mime,
    contentLength: Long
  ): Future[Response] = {
    assert(request != null, "null request")
    assert(in != null, "null input stream")
    assert(contentLength > 0, "positive content length is required")
    request.headers.set(CONTENT_LENGTH, contentLength)
    upload0(request, new ChunkedStream(in), contentType)
  }
  
  /**
   * Uploads from an NIO input channel using a streaming PUT operation.
   */
  def upload(
    request: HttpRequest,
    in: ReadableByteChannel,
    contentType: Mime,
    contentLength: Long
  ): Future[Response] = {
    assert(request != null, "null request")
    assert(in != null, "null input channel")
    assert(contentLength > 0, "positive content length is required")
    request.headers.set(CONTENT_LENGTH, contentLength)
    upload0(request, new ChunkedNioStream(in), contentType)
  }
  
  /**
   * A path for accessing Couch server configuration API operations. These
   * operations have URLs prefixed by "_config".
   */
  lazy val config = new CouchServerConfig(this)
  
  /**
   * A path for accessing utility resources (typically attachment files) such 
   * as those used by the couch  admin interface ("Futon"). These operations 
   * have URLs prefixed by "_utils".
   */
  lazy val utilFiles = new CouchServerUtilFiles(this)
  
  /**
   * A path for accessing Couch database API operations. These
   * operations have URLs prefixed by by the database name.
   */
  def database(name: String) = new CouchDatabase(this, name)
  
  override def toString() = this.getClass.getSimpleName + "[" + baseUrl + "]"
  
  
  //    ======================== API METHODS ========================
  
  /**
   * API METHOD: gets the couch MOTD and version.
   */
  def info(): Future[JsonObject] = fetchJsonObject(prepareGet())
  
  /**
   * API METHOD: gets the couch favicon.
   */
  def favicon(): Future[Array[Byte]] =
    bytes(prepareGet(baseUrl / "favicon.ico"))
  
  /**
   * API METHOD: gets the list of database names in the server.
   */
  def databaseNames(): Future[JsonArray] =
    fetchJsonArray(prepareGet(baseUrl / Path.ALL_DATABASES))
  
  /**
   * API METHOD: gets the list of currently active tasks in the server.
   */
  def activeTasks(): Future[JsonArray] =
    fetchJsonArray(prepareGet(baseUrl / Path.ACTIVE_TASKS))
  
  /**
   * API METHOD: gets the server statistics.
   */
  def stats(): Future[JsonObject] =
    fetchJsonObject(prepareGet(baseUrl / Path.STATS))
  
  /**
   * API METHOD: gets parts of the server log.
   */
  def log(
    bytesOption: Option[Long] = None,
    offsetOption: Option[Long] = None
  ): Future[String] = {
    import CouchClient.Param._
    bytesOption.foreach((n) => assert(n > 0))
    offsetOption.foreach((n) => assert(n > 0))
    var query: Map[String, String] = Map.empty
    bytesOption foreach { n => query = query + (BYTES -> n.toString) }
    offsetOption foreach { n => query = query + (OFFSET -> n.toString) }
    val req = prepareGet(baseUrl / Path.LOG & query)
    req.headers.set(Http.Header.ACCEPT, Mime.TEXT.toString)
    text(req)
  }
  
  /**
   * API METHOD: create a database.
   */
  def createDatabase(name: String): Future[CouchDatabase] = {
    fetchJsonObject(preparePut(baseUrl / name)) map { _ =>
      new CouchDatabase(this, name)
    }
  }
  
  /**
   * API METHOD: delete a database.
   */
  def deleteDatabase(name: String): Future[Unit]
    = fetchNothing(prepareDelete(baseUrl / name))
  
  /**
   * API METHOD: generate one or more UUIDs.
   */
  def generateUuids(count: Int = 1): Future[JsonObject] = {
    assert(count > 0)
    val query: Map[String, String] =
      if (count > 1) Map(Param.COUNT -> count.toString) else Map.empty
    fetchJsonObject(prepareGet(baseUrl / Path.UUIDS & query))
  }
  
  /**
   * API METHOD: restart the server.
   */
  def restartServer(): Future[JsonObject] =
    fetchJsonObject(preparePost(baseUrl / Path.RESTART))
  
  /**
   * API METHOD: begin a replicate operating (through the legacy 
   * "_replicate" interface).
   */
  def beginReplicate(
    source: String,
    target: String,
    createTarget: Boolean = false,
    continuous: Boolean = false,
    filterOpt: Option[CouchFilter] = None,
    docIds: Seq[String] = Nil,
    proxyUrlOpt: Option[String] = None
  ): Future[JsonObject] = {
    var bodyJson =
      Json(Param.SOURCE -> source, Param.TARGET ->target)
    if (createTarget == true) {
      bodyJson = bodyJson ++ Json(Param.CREATE_TARGET -> true)
    }
    if (continuous) {
      bodyJson = bodyJson ++ Json(Param.CONTINUOUS -> true)
    }
    filterOpt foreach { filter =>
      bodyJson = bodyJson ++ Json(Param.FILTER -> filter.toString)
    }
    if (!docIds.isEmpty) {
      val jsonDocIds = docIds.map(JsonString(_))
      bodyJson = bodyJson ++
        Json(Param.DOC_IDS -> Json(jsonDocIds:_*))
    }
    proxyUrlOpt foreach { proxyUrl =>
      bodyJson = bodyJson ++ Json(Param.PROXY -> proxyUrl)
    }
    fetchJsonObject(preparePost(bodyJson, baseUrl / Path.REPLICATE))
  }
  
  /**
   * API METHOD: cancel a replicate operating (through the legacy 
   * "_replicate" interface).
   */
  def cancelReplicate(
    source: String,
    target: String,
    continuous: Boolean = false
  ): Future[JsonObject] = {
    var bodyJson =
      Json(Param.SOURCE -> source, Param.TARGET ->target)
    if (continuous) {
      bodyJson = bodyJson ++ Json(Param.CONTINUOUS -> true)
    }
    bodyJson = bodyJson ++ Json(Param.CANCEL -> true)
    fetchJsonObject(preparePost(bodyJson, baseUrl / Path.REPLICATE))
  }
}
