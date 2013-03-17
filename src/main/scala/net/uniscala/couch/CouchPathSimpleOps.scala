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

import net.uniscala.json.{Json, JsonObject}

import Couch._
import util.{Http, Url}


/**
 * Provides Couch API methods for for simple operations on documents.
 */
trait CouchPathSimpleOps[C <: CouchDocBase] extends CouchPathOpsBase[C] {
  
  this: CouchSubPath =>
  
  import CouchDoc._
  
  def rev: String
  
  /**
   * A reference to this resource.
   */
  lazy val ref = CouchDoc.Rev(id, rev)
  
  /**
   * API METHOD: updates this document (using HTTP PUT).
   * Make 'batch' true to use "batch=ok" in the request.
   */
  def update(newJdoc: JsonObject, batch: Boolean = false): Future[C] = {
    
    // NOTE: there is a 'new_edits' mode - not sure if it would ever be used 
    // in a normal application, so we don't support it
    
    import Json._
    import CouchDoc.Param._
    assert(newJdoc != null, "null JSON document")
    val url = if (batch) {
      baseUrl & (BATCH -> OK)
    } else baseUrl
    val json: JsonObject = newJdoc ++ Json(Field.ID -> id, Field.REV -> rev)
    fetchInsertedDoc(preparePut(json, url), newJdoc)
  }
  
  /**
   * API METHOD: deletes this document (using HTTP DELETE).
   */
  def delete(): Future[Rev] =
    fetchRev(prepareDelete(baseUrl & Param.REV -> rev))
  
  /**
   * API METHOD: copies this document (using couch's non-standard 
   * HTTP COPY mechanism).
   * The target should be a list of URL segments that will be
   * encoded into the 'Location' header.
   */
  def copy(
    targetPath: List[String],
    targetRevOpt: Option[String] = None
  ): Future[Rev] = {
    val destinstionHeaderBuilder = new StringBuilder;
    {
      var first = true
      targetPath foreach { segment =>
        if (!first) destinstionHeaderBuilder append "/"
        first = false
        Url.encodeUrlPathSegment(segment, destinstionHeaderBuilder)
      }
    }
    targetRevOpt match {
      case Some(rev) => {
        destinstionHeaderBuilder.append('?')
        Url.encodeUrlQueryString(Param.REV, destinstionHeaderBuilder)
        destinstionHeaderBuilder.append('=')
        Url.encodeUrlQueryString(rev, destinstionHeaderBuilder)
      }
      case _ => { }
    }
    val req = prepareCopy()
    req.headers.set(
      Http.Header.DESTINATION,
      destinstionHeaderBuilder.toString
    )
    fetchRev(req)
  }
}
