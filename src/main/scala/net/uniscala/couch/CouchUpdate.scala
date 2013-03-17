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

import Couch._
import util.{Http, Mime}


private[couch] object CouchUpdate {
  
  private[couch] object HttpHeader {
    val NEW_REV = "X-Couch-Update-NewRev"
  }
}


/**
 * A path for invoking a couch update handler.
 * 
 * See: http://wiki.apache.org/couchdb/Document_Update_Handlers
 */
class CouchUpdate private[couch] (
  val shows: CouchUpdates,
  val name: String
) extends CouchSubPath(shows, name) {
  
  import scala.concurrent.ExecutionContext.Implicits.global
  
  import CouchUpdate._
  
  /**
   * Performs a Couch _update operation.
   * If successful, the return value is a pair containing the new or 
   * updated document's revision, if any, and the reply as an array of bytes.
   */
  private[couch] def update0(
    idOpt: Option[String] = None,
    options: Map[String, String] = Map.empty
  ): Future[(Option[String], Array[Byte])] = {
    
    val req = (idOpt map { id =>
      preparePut(baseUrl / id & options)
    } getOrElse {
      preparePost(baseUrl & options)
    })
    req.headers.set(Http.Header.ACCEPT, Mime.ANY.toString)
    
    client.send(req) flatMap { resp =>
      if (resp.statusCode / 100 == 4) {
        resp.text map { msg =>
          throw Couch.toCouchFailure(JsonParser.parseObject(msg))
        }
      } else {
        val revOpt = Option(resp.header(HttpHeader.NEW_REV))
        resp.bytes map { (revOpt, _) }
      }
    }
  }
  
  /**
   * API METHOD: invoke an update handler without a document.
   */
  def update(
    options: Map[String, String]
  ): Future[(Option[String], Array[Byte])] =
    update0(None, options)
  
  /**
   * API METHOD: invoke an update handler with a document.
   */
  def update(
    id: String,
    options: Map[String, String]
  ): Future[(Option[String], Array[Byte])] =
    update0(Some(id), options)
}
