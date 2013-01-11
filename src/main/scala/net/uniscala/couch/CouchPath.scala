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

import scala.concurrent.{ExecutionContext, Future}

import java.io.File

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.CharsetUtil.UTF_8

import net.uniscala.json._

import http.Client
import util.Http.Header._
import util.Mime
import util.Url
import Mime._

/**
 * Represents a resource or URL path in a Couch database. The 'path' part 
 * of the name refers to the URL path component that locates the resource.
 * Some such resources are not really concrete objects in the database: for
 * example, a CouchDesigns instance represents, say for a database called 
 * 'aaa', the path '/aaa/_design', and "contains" all of the design 
 * documents. Below that, you have CouchDesign instances, which refer to
 * individual design documents within the database.
 * <p>
 * A CouchPath implementation typically has a parent CouchPath, except for 
 * a CouchClient, which is the top level ("/"). Thus CouchPath
 * Implementations almost always inherits from CouchSubPath instead of 
 * CouchPath.
 */
trait CouchPath {
  
  import ExecutionContext.Implicits.global
  
  import Couch._
  import CouchClient._
  
  /**
   * The parent path of this path. The root path ("/"), which is a CouchClient,
   * has no parent (so this will be None).
   * 
   */
  val parentOption: Option[CouchPath]
  
  /**
   * A direct reference to the top-level path ("/", which is also the 
   * HTTP client for connecting to a couchdb server.
   */
  val client: CouchClient
  
  /**
   * The URL that this path represents.
   */
  def baseUrl: Url
  
  private def preparePutOrPost(
    bytes: Array[Byte],
    isPut: Boolean,
    url: Url
  ): HttpRequest = {
    val req = if (isPut) preparePut(url) else preparePost(url)
    req.setContent(Unpooled.wrappedBuffer(bytes))
    req.setHeader(CONTENT_TYPE, BINARY)
    req.setHeader(CONTENT_LENGTH, bytes.size)
    req
  }
  
  private[couch] def preparePutOrPost(
    json: JsonValue[_],
    isPut: Boolean,
    compact: Boolean,
    url: Url
  ): HttpRequest = {
    val jsonString = if (compact) json.toCompactString else json.toPrettyString
    val req = preparePutOrPost(jsonString.getBytes(UTF_8), isPut, url)
    req.setHeader(CONTENT_TYPE, JSON)
    req
  }
  
  private[couch] def prepareGet(): HttpRequest = prepareGet(baseUrl)
  
  private[couch] def prepareGet(path: String*): HttpRequest =
    prepareGet(baseUrl / (path:_*))
  
  private[couch] def prepareGet(url: Url): HttpRequest =
    Client.prepareGet(enc(url))
  
  private[couch] def preparePut(): HttpRequest = preparePut(baseUrl)
  
  private[couch] def preparePut(path: String*): HttpRequest =
    preparePut(baseUrl / (path:_*))
  
  private[couch] def preparePut(url: Url): HttpRequest =
    Client.preparePut(enc(url))
  
  private[couch] def preparePut(buf: ByteBuf, url: Url): HttpRequest = {
    val req = preparePut(url)
    req.setContent(buf)
    req
  }
  
  private[couch] def preparePut(bytes: Array[Byte], url: Url): HttpRequest =
    preparePutOrPost(bytes, true, url)
  
  private[couch] def preparePut(
    json: JsonValue[_],
    compact: Boolean,
    url: Url
  ): HttpRequest = preparePutOrPost(json, true, compact, url)
  
  private[couch] def preparePut(
    json: JsonValue[_],
    url: Url
  ): HttpRequest = preparePut(json, true, url)
  
  private[couch] def preparePost(): HttpRequest = preparePost(baseUrl)
  
  private[couch] def preparePost(path: String*): HttpRequest =
    preparePost(baseUrl / (path:_*))
  
  private[couch] def preparePost(url: Url): HttpRequest =
    Client.preparePost(enc(url))
  
  private[couch] def preparePost(content: ByteBuf, url: Url): HttpRequest = {
    val req = preparePost(url)
    req.setContent(content)
    req
  }
  
  private[couch] def preparePost(bytes: Array[Byte],url: Url): HttpRequest =
    preparePutOrPost(bytes, false, url)
  
  private[couch] def preparePost(
    json: JsonValue[_],
    compact: Boolean,
    url: Url
  ): HttpRequest = preparePutOrPost(json, false, compact, url)
  
  private[couch] def preparePost(
    content: JsonValue[_],
    url: Url
  ): HttpRequest = preparePost(content, true, url)
  
  private[couch] def prepareDelete(): HttpRequest = prepareDelete(baseUrl)
  
  private[couch] def prepareDelete(path: String*): HttpRequest =
    prepareDelete(baseUrl / (path:_*))
  
  private[couch] def prepareDelete(url: Url): HttpRequest =
    Client.prepareDelete(enc(url))
  
  private[couch] def prepareHead(): HttpRequest = prepareHead(baseUrl)
  
  private[couch] def prepareHead(path: String*): HttpRequest =
    prepareHead(baseUrl / (path:_*))
  
  private[couch] def prepareHead(url: Url): HttpRequest =
    Client.prepareHead(enc(url))
  
  private[couch] def prepareCopy(): HttpRequest = prepareCopy(baseUrl)
  
  private[couch] def prepareCopy(path: String*): HttpRequest =
    prepareCopy(baseUrl / (path:_*))
  
  private[couch] def prepareCopy(url: Url): HttpRequest =
    Client.prepareCopy(enc(url))
  
  private[couch] def fetchJson(req: HttpRequest): Future[JsonValue[_]] =
    client.text(req) map toSafeJsonObject
  
  private[couch] def fetchJsonObject(req: HttpRequest): Future[JsonObject] =
    client.text(req) map toSafeJsonObject
  
  private[couch] def fetchJsonArray(req: HttpRequest): Future[JsonArray] =
    client.text(req) map toSafeJsonArray
  
  private[couch] def fetchNothing(req: HttpRequest): Future[Unit] =
    fetchJsonObject(req) map { jobj => () }
  
  private[couch] def fetchRev(req: HttpRequest): Future[CouchDoc.Rev] =
    client.text(req) map { s =>
      import CouchDoc.Param._
      val jobj = toSafeJsonObject(s)
      val id0 = jobj(ID).value.toString
      val rev0 = jobj(REV).value.toString
      CouchDoc.Rev(id0, rev0)
  }
}
