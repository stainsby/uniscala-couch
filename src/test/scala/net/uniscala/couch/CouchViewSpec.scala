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


class CouchViewSpec extends Specification {
  
  // NOTE: most view functionality is tested thoroughly for temporary views,
  // so we only need a few basic tests here.
  
  sequential
  
  val dbName = "uniscala_couch_view_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db = CouchClientSpec.database(dbName)
  lazy val designs = db.designs
  
  val docId1 = "doc1"
  val docId2 = "doc2"
  val designId1 = "des1"
  val viewId = "view1"
  val mapFn = """
    function(doc) {
      emit([doc.foo[0], doc.foo[1]], doc.foo);
    }
  """
  val reduceFn = "_count"
  
  def setupView(): CouchView = {
    
    import Json._
    import CouchDoc._
    import CouchDoc.Implicits._
    
    await60 { db.insert(docId1, Json("foo" -> "bar")) }
    await60 { db.insert(docId2, Json("foo" -> "bing")) }
    await60 { designs.insert(
      designId1,
      Json(
        "views" -> Json(
          viewId -> Json(
            "map" -> mapFn,
            "reduce" -> reduceFn
          )
        )
      )
    ) }
    val design = db.design(designId1)
    design.view(viewId)
  }
  
  "view function" should {
    "apply a map function to docs" in {
      val docId = "doc1"
      val view = setupView()
      val res1 = orError(view.query(CouchViewOptions().reduce(false)))
      res1 must beRight and {
        val results = res1.right.get.toList
        results must haveSize(2) and {
          val values = results.flatMap(_.getAt[JsonString]("value").map(_.value))
          values must haveTheSameElementsAs(List("bar", "bing"))
        }
      }
    }
    "apply a reduce function to docs" in {
      val docId = "doc1"
      val view = setupView()
      val res1 = orError(view.query())
      res1 must beRight and {
        val results = res1.right.get.toList
        results must haveSize(1) and {
          val values =
            results.flatMap(_.getAt[JsonInteger]("value").map(_.value))
          values must haveTheSameElementsAs(List(2L))
        }
      }      
    }
    "allow POSTing of keys" in {
      import Json._
      val docId = "doc1"
      val view = setupView()
      val key = Json("b", "i")
      val res1 = orError(
        view.queryPost(
          Json(key),
          CouchViewOptions().reduce(false)
        )
      )
      res1 must beRight and {
        val results = res1.right.get.toList
        results must haveSize(1) and {
          val values = results.flatMap(
            _.getAt[JsonString]("value").map(_.value)
          )
          values must haveTheSameElementsAs(List("bing"))
        }
      }
    }
  }
}