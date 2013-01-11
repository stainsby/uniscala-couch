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

import net.uniscala.json.JsonParser

import util.{Http, Mime}

import Couch._


/**
 * A path for querying a couch show.
 */
class CouchShow private[couch] (val shows: CouchShows, val name: String)
extends CouchSubPath(shows, name) {
  
  import scala.concurrent.ExecutionContext.Implicits.global
  
  
  // API methods
  
  
  /**
   * API METHOD: query the show. This method allow arbitrary query parameters 
   * to be sent in the requests query string.
   */
  private[couch] def query(
    docIdOpt: Option[String],
    queryParams: Map[String, String] = Map.empty
  ): Future[Array[Byte]] = {
    
    var url = baseUrl & queryParams
    docIdOpt foreach { docId => url = url / docId }
    val req = prepareGet(url)
    req.setHeader(Http.Header.ACCEPT, Mime.ANY.toString)
    
    client.send(req) flatMap  { resp =>
      resp.bytes map { content =>
        if (resp.statusCode / 100 == 4) {
          val msg = new String(content, "utf8")
          throw Couch.toCouchFailure(JsonParser.parseObject(msg))
        } else {
         content
        }
      }
    }
  }
  
  /**
   * API METHOD: query the show for a particular document.
   */
  def query(docId: String): Future[Array[Byte]] = query(Some(docId))
  
  /**
   * API METHOD: query the show without a document.
   */
  def query(): Future[Array[Byte]] = query(None, Map[String, String]())
  
  /**
   * API METHOD: query the show without a document. This version of the 
   * query method allows arbitrary query parameters to be passed.
   */
  def query(queryParams: Map[String, String]): Future[Array[Byte]] =
    query(None, queryParams)
  
  /**
   * API METHOD: query the show for a particular document. This version of the 
   * query method allows arbitrary query parameters to be passed.
   */
  def query(
    docId: String,
    queryParams: Map[String, String]
  ): Future[Array[Byte]] =
    query(Some(docId), queryParams)
}
