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

import scala.concurrent.Future

import net.uniscala.json._

import Couch._
import util.Url


object CouchView {
  
  private[couch] object Field {
    val MAP = "map"
    val REDUCE = "reduce"
    val ROWS = "rows"
  }
  
  private[couch] object Param {
    val DESCENDING          = "descending"
    val END_KEY             = "endkey"
    val END_KEY_DOC_ID      = "endkey_docid"
    val GROUP               = "group"
    val GROUP_LEVEL         = "group_level"
    val INCLUDE_DOCS        = "include_docs"
    val INCLUSIVE_END       = "inclusive_end"
    val KEY                 = "key"
    val KEYS                = "keys"
    val LIMIT               = "limit"
    val REDUCE              = "reduce"
    val SKIP                = "skip"
    val STALE               = "stale"
    val STALE_OK            = "ok"
    val STALE_UPDATE_AFTER  = "update_after"
    val START_KEY           = "startkey"
    val START_KEY_DOC_ID    = "startkey_docid"
    val UPDATE_SEQ          = "update_seq"
  }
  
  import scala.language.existentials
  case class Row(id: Option[String], key: JsonValue[_], value: JsonValue[_])
}

/**
 * A path for querying a couch view.
 */
class CouchView private[couch] (val views: CouchViews, val name: String)
extends CouchSubPath(views, name){
  
  /**
   * API METHOD: performs a view query (by HTTP GET).
   * 
   * As well as standard options, arbitrary options can also be appended 
   * to the query string using 'otherOptions'.
   */
  def query(
    options: CouchViewOptions = CouchViewOptions(),
    otherOptions: Map[String, String] = Map.empty
  ): Future[CouchResultIterator] = {
    val url: Url = options.toUrl(baseUrl) & (otherOptions)
    CouchResultIterator(client, prepareGet(url))
  }
  
  /**
   * API METHOD: performs a view query (by HTTP POST), including specific 
   * view keys.
   * 
   * As well as standard  options, arbitrary options can also be appended to 
   * the query string using 'otherOptions'.
   */
  def queryPost(
    keys: JsonArray,
    options: CouchViewOptions = CouchViewOptions(),
    otherOptions: Map[String, String] = Map.empty
  ): Future[CouchResultIterator] = {
    val url: Url = options.toUrl(baseUrl) & (otherOptions)
    val body = Json(CouchView.Param.KEYS -> keys)
    CouchResultIterator(client, preparePost(body, url))
  }
}
