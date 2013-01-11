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

import org.specs2.mutable._
import org.specs2.specification.Scope
import org.specs2.specification.Outside

import net.uniscala.json._

import Futures._


class CouchShowSpec extends Specification {
  
  sequential
  
  val dbName = "uniscala_couch_show_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db = CouchClientSpec.database(dbName)
  lazy val designs = db.designs
  
  val showFn = """
    function(doc, req) {
      if (doc) {
        if (doc.foo) {
          return doc.foo
        } else {
          return "NOFOO"
        }
      } else { // document not found
        if(req.id) {
          return req.id
        } else {
          return req.query.param1
        }
      }
    }
  """
  
  def setupShow(): CouchShow = {
    
    import Json._
    
    val docId = "doc1"
    val designId = "des1"
    val showId = "show1"
    
    await60(db.insert(docId, Json("foo" -> "bar")))
    await60(designs.insert(
      designId,
      Json(
        "shows" -> Json(
          showId -> showFn
        )
      )
    ))
    val design = db.design(designId)
    design.show(showId)
  }
    
  "show function" should {
    "show with ID for a pre-existing doc" in {
      val docId = "doc1"
      val show = setupShow()
      val res1 = orError(show.query(docId, Map.empty[String, String]))
      res1 must beRight and {
        val shown: String = new String(res1.right.get)
        shown must beEqualTo("bar")
      }
    }
    "show with ID but no doc" in {
      val docId = "newdoc1"
      val show = setupShow()
      val res1 = orError(show.query(docId, Map.empty[String, String]))
      res1 must beRight and {
        val shown: String = new String(res1.right.get)
        shown must beEqualTo(docId)
      }
    }
    "show with no ID and pass params" in {
      val docId = "newdoc1"
      val show = setupShow()
      val res1 = orError(show.query(Map("param1" -> "baggins")))
      res1 must beRight and {
        val shown: String = new String(res1.right.get)
        shown must beEqualTo("baggins")
      }
    }
  }
}
