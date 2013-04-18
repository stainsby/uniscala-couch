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


class CouchDesignsSpec extends Specification {
  
  sequential
  
  val dbName = "uniscala_couch_designs_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db: CouchDatabase = CouchClientSpec.database(dbName)
  lazy val designs: CouchDesigns = db.designs
  
  "CouchDesigns" should {
    
    import Json._
    
    "be able to create a design doc" in {
      val ddoc = Json("foo" -> "bar")
      val res1 = orError(designs.insert("mydesign", ddoc))
      (res1 must beRight) and {
        val doc: CouchDoc = res1.right.get
        doc.id must beEqualTo("_design/mydesign") and {
          doc.json must beEqualTo(ddoc)
        }
      }
    }
  }
}
