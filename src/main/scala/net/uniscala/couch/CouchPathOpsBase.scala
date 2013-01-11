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

import io.netty.handler.codec.http.HttpRequest

import net.uniscala.json.JsonObject


/**
 * The base class for traits embodying sets of operations at a 'path'
 * (see CouchPath).
 */
trait CouchPathOpsBase[C <: CouchDocBase] {
  
  this: CouchSubPath =>
  
  import ExecutionContext.Implicits.global
  
  /**
   * Instantiates a new document. Concrete implmentors should return
   * an instance appropriate for this path.
   */
  protected def newDoc(id: String, rev: String, jdoc: JsonObject): C
  
  private[couch] def fetchInsertedDoc(
    req: HttpRequest,
    jdoc: JsonObject
  ): Future[C] = {
    
    import CouchDoc.Param._
    
    fetchJsonObject(req) map { jobj =>
      val id0 = jobj(ID).value.toString
      val rev0 = jobj(REV).value.toString
      newDoc(id0, rev0, jdoc)
    }
  }
  
  private[couch] def fetchDoc(req: HttpRequest): Future[C] = {
    
    import CouchDoc.Field._
    
    fetchJsonObject(req) map { jobj =>
      val id0 = jobj(ID).value.toString
      val rev0 = jobj(REV).value.toString
      newDoc(id0, rev0, jobj)
    }
  }
  
  private[couch] def fetchDocOption(req: HttpRequest): Future[Option[C]] = {
    fetchDoc(req) map { Some(_) }  recover {
      case CouchFailure(Couch.Field.NOT_FOUND, _) => None
    }
  }
}
