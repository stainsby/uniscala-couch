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

import java.io.InputStream

import net.uniscala.json.{Json, JsonArray}

import util._


/**
 * A path for applying a couch list to a specific couch view.
 */
class CouchList private[couch] (
  val lists: CouchLists,
  id: String,
  val view: CouchView
) extends CouchSubPath(lists, id) {
  
  import ExecutionContext.Implicits.global
  
  import Couch._
  import Http.Header._
  
  override lazy val baseUrl = lists.baseUrl / id
  
  
  // API METHODS
  
  
  private def query0[T](
    keysOpt: Option[JsonArray] = None,
    options: CouchViewOptions = CouchViewOptions(),
    contentType: Mime = Mime.BINARY,
    otherOptions: Map[String, String] = Map.empty
  ): Future[InputStream] = {
    
    var url: Url = options.toUrl(baseUrl) & (otherOptions)
    // support views belonging to other design docs
    url =  if (view.views.design == this.lists.design) {
      url / view.name
    } else {
      url / view.views.design.id / view.name
    }
    val req = keysOpt map { keys =>
      preparePost(Json(CouchView.Param.KEYS -> keys), url)
    } getOrElse prepareGet(url)
    req.headers.set(ACCEPT, contentType.toString)
    client.send(req) flatMap { resp =>
      val statusCode = resp.statusCode
      if (statusCode == 200) {
        resp.readStream(in => in)
      } else {
        throw Couch.toCouchFailure(statusCode, resp.statusMessageOption)
      }
    }
  }
  
  /**
   * API METHOD: performs a list query (by HTTP GET).
   */
  def query(
    options: CouchViewOptions = CouchViewOptions(),
    contentType: Mime = Mime.BINARY,
    otherOptions: Map[String, String] = Map.empty
  ): Future[InputStream] = 
    query0(None, options, contentType, otherOptions)
  
  /**
   * API METHOD: performs a list query (by HTTP POST).
   * Specific view keys can be passed in this type of query.
   */
  def queryPost(
    keys: JsonArray,
    options: CouchViewOptions = CouchViewOptions(),
    contentType: Mime = Mime.BINARY,
    otherOptions: Map[String, String] = Map.empty
  ): Future[InputStream] = 
    query0(Some(keys), options, contentType, otherOptions)
}
