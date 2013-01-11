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

import io.netty.handler.codec.http.HttpHeaders

import net.uniscala.json.{JsonString, JsonObject}

import Couch._
import util.Url


/**
 * Provides Couch API methods for parent paths. (see CouchPath).
 */
trait CouchPathContainerOps[C <: CouchDocBase] extends CouchPathOpsBase[C] {
  
  this: CouchSubPath =>
  
  import CouchDoc._
  
  import ExecutionContext.Implicits.global
  
  /**
   * API METHOD: Gets a document.
   */
  def get(
    id: String,
    revOpt: Option[String] = None,
    extraFields: Set[CouchDoc.ExtraFieldOption] = Set.empty
  ): Future[Option[C]] = {
    assert(id != null, "null ID")
    assert(revOpt != null && revOpt.map(_ != null).getOrElse(true), "null rev")
    var url: Url = baseUrl / id
    extraFields foreach { field =>
      url = url & field.name -> true.toString
    }
    revOpt foreach { rev =>
      assert(rev != null && rev.length > 0)
      url = url & CouchDoc.Param.REV -> rev
    }
    fetchDocOption(prepareGet(url))
  }
  
  /**
   * API METHOD: Gets a info about document using and HEAD HTTP request.
   * The resulting header fields are returned.
   */
  def head(id: Id): Future[Option[Seq[(String, String)]]] = {
    assert(id != null, "null ID")
    client.send(prepareHead(id.id))map { resp =>
      Some(resp.headers)
    } recover {
      case CouchFailure(Couch.Field.NOT_FOUND, _) => None
    }
  }
  
  private def insert0(
    idOpt: Option[String] = None,
    jdoc: JsonObject,
    batch: Boolean = false
  ): Future[C] = {
    
    // NOTE: there is a 'new_edits' mode - not sure if it would ever be used 
    // in a normal application, so we don't support it
    
    import CouchDoc.Param._
    assert(jdoc != null, "null JSON doc")
    val url = if (batch) { baseUrl & (BATCH -> OK) } else baseUrl
    val req = (
      idOpt map { id: String =>
        assert(id != null && id.length > 0, "invalid ID")
        preparePut(jdoc, url / id)
      } getOrElse {
        preparePost(jdoc, url)
      }
    )
    fetchInsertedDoc(req, jdoc)
  }
  
  /**
   * API METHOD: Inserts a document using HTTP POST.
   */
  def insert(
    jdoc: JsonObject
  ): Future[C] = {
    insert0(None, jdoc, false)
  }
  
  /**
   * API METHOD: Inserts a document using HTTP POST and with 'batched=ok'
   */
  def insertBatched(
    jdoc: JsonObject
  ): Future[C] = {
    insert0(None, jdoc, true)
  }
  
  /**
   * API METHOD: Inserts a document using HTTP PUT.
   */
  def insert(
    id: String,
    jdoc: JsonObject
  ): Future[C] = {
    insert0(Some(id), jdoc, false)
  }
  
  /**
   * API METHOD: Inserts a document using HTTP PUT and with 'batched=ok'
   */
  def insertBatched(
    id: String,
    jdoc: JsonObject
  ): Future[C] = {
    insert0(Some(id), jdoc, true)
  }
  
  // TODO: support preserving of fields by updating with "_deleted": true?
  /**
   * API METHOD: Deletes a document using HTTP DELETE.
   */
  def delete(rev: Rev): Future[Rev] =
    fetchRev(prepareDelete(baseUrl / rev.id & Param.REV -> rev.rev))
}
