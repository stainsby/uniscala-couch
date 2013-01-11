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


class CouchListSpec extends Specification {
  
  // NOTE: most view functionality is tested thoroughly for temporary views,
  // so we only need a few basic tests here.
  
  sequential
  
  val dbName = "uniscala_couch_list_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db = CouchClientSpec.database(dbName)
  lazy val designs = db.designs
  
  val docId1 = "doc1"
  val docId2 = "doc2"
  val docId3 = "doc3"
  val designId1 = "des1"
  val designId2 = "des2"
  val viewId1 = "view1"
  val viewId2 = "view2"
  val listId1 = "list1"
  val mapFn1 = """
    function(doc) {
      emit(doc.foo[0], doc.foo);
    }
  """
  val mapFn2 = """
    function(doc) {
      emit(doc.foo[0], doc.foo.slice(0,2));
    }
  """
  val listFn1 = """
    function(head, req) {
      var row;
      start({
        "headers": {
          "Content-Type": "text/plain"
         }
      });
      while(row = getRow()) {
        send(row.value + "\n");
      }
    }
  """
  
  def setupDesigns(): (CouchDesign, CouchDesign) = {
    
    import Json._
    
    await60(db.insert(docId1, Json("foo" -> "ant")))
    await60(db.insert(docId2, Json("foo" -> "bat")))
    await60(db.insert(docId3, Json("foo" -> "car")))
    await60(designs.insert(
      designId1,
      Json(
        "views" -> Json(
          viewId1 -> Json(
            "map" -> mapFn1
          )
        ),
        "lists" -> Json(
          listId1 -> listFn1
        )
      )
    ))
    await60(designs.insert(
      designId2,
      Json(
        "views" -> Json(
          viewId2 -> Json(
            "map" -> mapFn2
          )
        )
      )
    ))
    val design1 = db.design(designId1)
    val design2 = db.design(designId2)
    (design1, design2)
  }
  
  "list function" should {
    "list a view in the same design doc" in {
      val docId = "doc1"
      val design = setupDesigns()._1
      val view = design.view(viewId1)
      val list = design.list(listId1, view)
      val res1 = orError(list.query())
      res1 must beRight and {
        val in = res1.right.get
        val bytes =
          Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
        val str = new String(bytes)
        str must beEqualTo("ant\nbat\ncar\n")
      }
    }
    "list a view in the same design doc with POSTed IDs" in {
      import Json._
      val docId = "doc1"
      val design = setupDesigns()._1
      val view = design.view(viewId1)
      val list = design.list(listId1, view)
      val res1 = orError(list.queryPost(Json("b", "c", "d")))
      res1 must beRight and {
        val in = res1.right.get
        val bytes =
          Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
        val str = new String(bytes)
        str must beEqualTo("bat\ncar\n")
      }
    }
    "list a view in a different design doc" in {
      val docId = "doc1"
      val designPair = setupDesigns()
      val design1 = designPair._1
      val design2 = designPair._2
      val view2 = design2.view(viewId2)
      val list = design1.list(listId1, view2)
      val res1 = orError(list.query())
      res1 must beRight and {
        val in = res1.right.get
        val bytes =
          Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
        val str = new String(bytes)
        str must beEqualTo("an\nba\nca\n")
      }
    }
  }
}
