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


class CouchUpdateSpec extends Specification {
  
  sequential
  
  val dbName = "uniscala_couch_update_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db = CouchClientSpec.database(dbName)
  lazy val designs = db.designs
  
  val updateFn = """
    function(doc, req) {
      if (!doc) {
        if (req.id) {
          return [{
            _id : req.id,
            param1: req.query.param1
          }, 'created doc']
        }
        return [null, 'noop'];
      }
      doc.param1 = req.query.param1;
      return [doc, 'updated doc'];
    }
  """
  
  def setupUpdate(): CouchUpdate = {
    
    import Json._
    
    val docId = "doc1"
    val designId = "des1"
    val updateId = "update1"
    
    orError(db.insert(docId, Json("foo" -> "bar"))).right.get
    orError(designs.insert(
      designId,
      Json(
        "updates" -> Json(
          updateId -> updateFn
        )
      )
    )).right.get
    val design = db.design(designId)
    design.update(updateId)
  }
  
  "update function" should {
    "update a pre-existing doc" in {
      val docId = "doc1"
      val update = setupUpdate()
      val res1 = orError(update.update(docId, Map("param1" -> "hiccup")))
      res1 must beRight and {
        val reply = res1.right.get
        val msg: String = new String(reply._2)
        msg must beEqualTo("updated doc") and {
          val revOpt = reply._1
          revOpt must beSome and {
            val rev = revOpt.get
            val doc = await60(db.get(docId)).get
            doc.rev must beEqualTo(rev) and {
              doc.json.getAt[JsonString]("param1") must
                beSome(JsonString("hiccup"))
            }
          }
        }
      }
    }
    
    "create a new doc" in {
      val docId = "doc2"
      val update = setupUpdate()
      val res1 = orError(update.update(docId, Map("param1" -> "fishlegs")))
      res1 must beRight and {
        val reply = res1.right.get
        val msg: String = new String(reply._2)
        msg must beEqualTo("created doc") and {
          val revOpt = reply._1
          revOpt must beSome and {
            val rev = revOpt.get
            val doc = await60(db.get(docId)).get
            doc.rev must beEqualTo(rev) and {
              doc.json.getAt[JsonString]("param1") must
                beSome(JsonString("fishlegs"))
            }
          }
        }
      }
    }
    
    "do no operations" in {
      val update = setupUpdate()
      val res1 = orError(update.update(Map.empty))
      res1 must beRight and {
        val reply = res1.right.get
        val msg: String = new String(reply._2)
        msg must beEqualTo("noop") and {
          val revOpt = reply._1
          revOpt must beNone
        }
      }
    }
  }
}
