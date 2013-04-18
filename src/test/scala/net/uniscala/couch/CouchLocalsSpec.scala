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


class CouchLocalsSpec extends Specification {
  
  sequential
  
  val dbName = "uniscala_couch_locals_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db: CouchDatabase = CouchClientSpec.database(dbName)
  lazy val locals: CouchLocals = db.locals
  
  "locals and local docs" should {
    
    // Support for local docs uses CouchPathContainerOps and 
    // CouchPathSimpleOps, which are tested thoroughly elsewhere, so we 
    // just do some quick tests here.
    
    import Json._
    
    "be able to do simple insert, copy, update and delete" in {
      
      val id = "mylocal"
      val fullId = "_local/" + id
      val jdoc = Json("foo" -> "bar")
      val localDoc = await60(locals.insert(id, jdoc))
      
      def getField(doc: CouchDocBase, key: String): String =
        doc.json.getAt[JsonString](key).get.value
      
      localDoc.id must beEqualTo(fullId) and {
        localDoc.json must beEqualTo(jdoc) and {
          val localDocViaGet = await60(locals.get(id)).get
          localDocViaGet.id must beEqualTo(fullId) and {
            getField(localDocViaGet, "foo") must beEqualTo("bar") and {
              val id2 = "mylocal2"
              val fullId2 = "_local/" + id2
              await60(localDocViaGet.copy("_local" :: id2 :: Nil))
              val localDoc2ViaGet = await60(locals.get(id2)).get
              localDoc2ViaGet.id must beEqualTo(fullId2) and {
                getField(localDoc2ViaGet, "foo") must beEqualTo("bar") and {
                  val jdoc2 = Json("Frodo" -> "Baggins")
                  await60(localDoc2ViaGet.update(Json("Frodo" -> "Baggins")))
                  val localDoc2ViaGet2 = await60(locals.get(id2)).get
                  localDoc2ViaGet2.id must beEqualTo(fullId2) and {
                    getField(localDoc2ViaGet2, "Frodo") must beEqualTo("Baggins") and {
                      await60(localDoc2ViaGet2.delete())
                      val localDoc2ViaGet3Opt = await60(locals.get(id2))
                      localDoc2ViaGet3Opt must beNone
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
