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

import util.Mime

import Futures._


class CouchDocSpec extends Specification {
  
  import Couch._
  import CouchDoc._
  
  sequential
  
  val dbName = "uniscala_couch_doc_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db: CouchDatabase = CouchClientSpec.database(dbName)
  
  "deleting a document" should {
    "return a valid document id/rev" in {
      import CouchDatabaseSpec.JsonExamples._
      val res0 = orError(db.insert(shop1))
      res0 must beRight and {
        val doc1: CouchDoc = res0.right.get
        val res = orError(doc1 delete())
        res must beRight and {
          val rev: Rev = res.right.get
          (rev must not beNull) and
          (rev.rev must not beNull) and
          (rev.rev.length must beGreaterThan(0))
        }
      }
    }
  }
  
  "updating a document" should {
    "return a valid document id/rev and updated fields" in {
      import CouchDatabaseSpec.JsonExamples._
      import Json._
      val res0 = orError(db.insert(shop1))
      res0 must beRight and {
        val doc1: CouchDoc = res0.right.get
        val newName: String = "Acme Shop"
        val newValue = doc1.jdoc.replace(JsonPath.root / "name" -> newName);
        val res = orError(doc1.update(newValue))
        res must beRight and {
          val doc2: CouchDoc = res.right.get
          (doc2 must not beNull) and
          (doc2.ref must not beNull) and
          (doc2.ref .rev must not beNull) and
          (doc2.ref .rev.length must beGreaterThan(0)) and {
            val res2 = orError(db.get(doc2.id))
            res2 must beRight
          }
        }
      }
    }
    "work when fetching a fresh copy" in {
      import Couch._
      import Json._
      val id: String = await60(db.insert(Json("aaa" -> 111))).id
      val doc1: CouchDoc = await60(db.get(id)).get
      await60(doc1.update(Json("hover" -> "craft")))
      val doc2: CouchDoc = await60(db.get(id)).get
      val v1 = doc2.jdoc.getAt[JsonInteger]("aaa").map(_.value)
      val v2 = doc2.jdoc.getAt[JsonString]("hover").map(_.value)
      (v1 must beNone) and (v2 must beSome("craft"))
    }
  }
  
  "copying a document" should {
    import CouchDatabaseSpec.JsonExamples._
    import Json._
    "return a valid document id/rev" in {
      val res1 = orError(db.insert(shop1))
      res1 must beRight and {
        val originalDoc: CouchDoc = res1.right.get
        val copyId = CouchClientSpec.generateRandomId()
        val res2 = orError(originalDoc.copy(copyId :: Nil))
        res2 must beRight and {
          val copiedDocRev = res2.right.get
          copiedDocRev.id must beEqualTo(copyId)
          val res3 = orError(db.get(copyId))
          res3 must beRight and {
            val copiedDocOpt = res3.right.get
            copiedDocOpt must beSome and {
              val copiedDoc = copiedDocOpt.get
              val copyRev = copiedDocRev.rev
              val res2 = orError(originalDoc.copy(copyId :: Nil, Some(copyRev)))
              res2 must beRight and {
                val copiedDocRev2 = res2.right.get
                val copyRev2 = copiedDocRev2.rev
                (copyRev2 !== copyRev) and (copiedDocRev2.id === copyId)
              }
            }
          }
        }
      }
    }
  }
  
  "attaching and deleting a file to a document" should {
    "store the file data and then remove it" in {
      import CouchDatabaseSpec.JsonExamples._
      import CouchDatabaseSpec.AttachmentExamples._
      val attachmentFileName = file1
      val attachmentFilePath = path1
      val res0 = orError(db.insert("mydoc", shop1))
      res0 must beRight and {
        val doc: CouchDoc = res0.right.get;
        val file = new java.io.File(attachmentFilePath);
        val res1 = orError(doc.attach(attachmentFileName, file, Mime.PNG))
        (res1 must beRight) and {
          val rev: Rev = res1.right.get;
          val res2 = orError(db.get(rev.id))
          (res2 must beRight) and {
            val docV2Opt: Option[CouchDoc] = res2.right.get;
            docV2Opt must beSome and {
              val docV2 = docV2Opt.get
              val attachmtRes = orError(docV2.attachment(attachmentFileName))
              attachmtRes must beRight and {
                val res3 = orError(docV2.deleteAttachment(attachmentFileName))
                res3 must beRight and {
                  val rev3: Rev = res3.right.get
                  val res4 = orError(db.get(rev3.id))
                  res4 must beRight and {
                    val docV3Opt = res4.right.get
                    docV3Opt must beSome and {
                      val docV3 = docV3Opt.get
                      val attmt2 = orError(docV3.attachment(attachmentFileName))
                      attmt2 must beRight and {
                        attmt2.right.get must beNone
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
}