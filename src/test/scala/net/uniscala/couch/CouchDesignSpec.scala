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

import java.io.File

import org.specs2.mutable._
import org.specs2.specification.Scope
import org.specs2.specification.Outside

import net.uniscala.json._

import util.Mime

import Futures._


object CouchDesignSpec {
  
  object Design {
    val DESIGN1 = "design1"
  }
  
  class Cleanups(dbName: String) extends CouchClientSpec.Cleanups(dbName) {
    override def before() = {
      super.before()
      await60(couch.database(dbName).designs.insert(Design.DESIGN1, JsonObject()))
    }
  }
}


class CouchDesignSpec extends Specification {
  
  import Couch._
  
  sequential
  
  import CouchDesignSpec.Design._
  
  val dbName = "uniscala_couch_design_test"
  lazy val db: CouchDatabase = CouchClientSpec.database(dbName)
  lazy val couch = CouchClientSpec.couch
  implicit lazy val myContext = new CouchDesignSpec.Cleanups(dbName)
  lazy val designs: CouchDesigns = db.designs
  
  "design doc" should {
    
    import Json._
    
    "be able to be updated" in {
      val ddoc1 = Json("foo" -> "bar")
      val res1 = orError(designs.insert("mydesign/2", ddoc1))
      (res1 must beRight) and {
        val doc: CouchDoc = res1.right.get;
        val ddoc2 = JsonObject("meow" -> "fancy")
        val res2 = orError(doc.update(ddoc2))
        val res3 = orError(designs.get("mydesign/2"))
        (res3 must beRight) and {
          val updatedDocOpt = res3.right.get
          (updatedDocOpt must beSome) and {
            val updatedDoc = updatedDocOpt.get;
            (updatedDoc.id must beEqualTo("_design/mydesign/2")) and {
              updatedDoc.jdoc.getAt[JsonString]("meow") must
                beSome(JsonString("fancy"))
            }
          }
        }
      }
    }
    
    "be able to be deleted" in {
      val ddoc1 = JsonObject("foo" -> "bar")
      val res1 = orError(designs.insert("mydesign/3", ddoc1))
      (res1 must beRight) and {
        val doc: CouchDoc = res1.right.get
        val ddoc2 = JsonObject("meow" -> "fancy")
        val res2 = orError(doc.delete())
        val res3 = orError(designs.get("mydesign/3"))
        (res2 must beRight) and (res3 must beRight) and {
          val docOpt = res3.right.get
          docOpt must beNone
        }
      }
    }
    
    "be able to be copied" in {
      val ddoc1 = JsonObject("ice" -> "berg");
      val res1 = orError(designs.insert("mydesign4", ddoc1))
      (res1 must beRight) and {
        val doc: CouchDoc = res1.right.get
        val res2 = orError(doc.copy("_design" :: "mydesign4copy" :: Nil))
        val res3 = orError(designs.get("mydesign4copy"))
        (res3 must beRight) and {
          val copiedDocOpt: Option[CouchDoc] = res3.right.get
          (copiedDocOpt must beSome) and {
            val copiedDoc = copiedDocOpt.get;
            copiedDoc.jdoc.getAt[JsonString]("ice") must
              beSome(JsonString("berg"))
          }
        }
      }
    }
    
    "be able to have an attachment added and removed" in {
      import CouchDatabaseSpec.JsonExamples._
      import CouchDatabaseSpec.AttachmentExamples._
      val attachmentFileName = file1
      val attachmentFilePath = path1
      val res0 = orError(designs.insert("mydesign5", shop1))
      (res0 must beRight) and {
        val doc: CouchDoc = res0.right.get
        val file = new File(attachmentFilePath)
        val res1 = orError(doc.attach(attachmentFileName, file, Mime.PNG))
        (res1 must beRight) and {
          val res2 = orError(doc.attachment(attachmentFileName))
          (res2 must beRight) and {
            val attachmentOpt = res2.right.get;
            (attachmentOpt must beSome) and {
              val attachment = attachmentOpt.get;
              val in = attachment.stream;
              (in.read() must beGreaterThan(-1)) and {
                in.close
                val docOpt = await60(designs.get("mydesign5"))
                (docOpt must beSome) and {
                  val doc3 = docOpt.get
                  val res3 = orError(doc3.deleteAttachment(attachmentFileName))
                  res3 must beRight and {
                    res3.right.get.id must beEqualTo("_design/mydesign5") and {
                      val res4 = orError(doc3.attachment(attachmentFileName))
                      res4 must beRight and {
                        val attachmentOpt = res4.right.get
                        attachmentOpt must beNone
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
  
  "design doc info" should {
    "have some special values" in {
      val des1 = db.design(DESIGN1)
      val infoRes = orError(des1.info())
      infoRes must beRight and {
        val info = infoRes.right.get
        (info.get("name") must beSome) and
          (info.get("view_index") must beSome)
      }
    }
  }
  
  "compact views" should {
    "return with no errors" in {
      val des1 = db.design(DESIGN1)
      val errorOpt = orError(des1.compactViews())
      errorOpt must beRight
    }
  }
}