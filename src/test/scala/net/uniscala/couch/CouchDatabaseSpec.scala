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

import scala.util.{Failure, Success, Try}

import org.specs2.mutable._
import org.specs2.specification.Scope
import org.specs2.specification.Outside

import net.uniscala.json._

import Futures._


object CouchDatabaseSpec {
  
  object Models {
    case class Shop(name: String, location: String, postcode: Int)
  }
  
  object JsonExamples {
    
    import Json._
    
    val shop1: JsonObject = Json(
      "type" -> "shop",
      "name" -> "MyShop",
      "location" -> "Melbourne",
      "postcode" -> 4567
    )
    
    val user1 = Json(
      "type" -> "user",
      "profiles" -> Json(
        "mybook"   -> Json("key" -> "AGW45HWH", "secret" -> "g4juh43ui9g929k4"),
        "alt"      -> Json("key" -> "ER45DFE3", "secret" -> "0867986769de68")        
      ),
      "and" -> 123,
      "even" -> 456,
      "more" -> Json("uninteresting" -> 678, "stuff" -> 999)
    )
  }
  
  object AttachmentExamples {
    val file1 = "badge.png";
    val path1 = System.getProperty("user.dir") +
      "/src/test/resources/net/uniscala/couch/" + file1
  }
}


class CouchDatabaseSpec extends Specification {
  
  import CouchClientSpec.generateRandomId
  import CouchDatabaseSpec._
  
  sequential
  
  val dbName = "uniscala_couch_database_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db: CouchDatabase = CouchClientSpec.database(dbName)
  
  "inserting a document with no ID (POST)" should {
    "have no errors" in {
      import CouchDatabaseSpec.JsonExamples._
      val res = orError(db.insert(shop1))
      (res must beRight) and {
        val doc = res.right.get
        (doc.id must not beNull) and
        (doc.id.length must beGreaterThan(0)) and
        (doc.rev must not beNull) and
        (doc.rev.length must beGreaterThan(0))
      }
    }
  }
  
  "inserting a document with ID (PUT)" should {
    "have no errors" in {
      import CouchDatabaseSpec.JsonExamples._
      val res = orError(db.insert(generateRandomId(), shop1))
      (res must beRight) and {
        val doc = res.right.get
        (doc.id must not beNull) and
        (doc.id.length must beGreaterThan(0)) and
        (doc.rev must not beNull) and
        (doc.rev.length must beGreaterThan(0))
      }
    }
  }
  
  "getting an existing document by id" should {
    "return a valid document id/rev" in {
      import CouchDatabaseSpec.JsonExamples._
      val res0 = orError(db.insert(shop1))
      res0 must beRight and {
        val doc1: CouchDoc = res0.right.get
        val ref = doc1.ref
        val res = orError(db.get(ref.id))
        (res.left.toOption must beNone) and
        (res.right.toOption must beSome) and {
          val docOpt: Option[CouchDoc] = res.right.get
          (docOpt must beSome) and {
            val doc: CouchDoc = docOpt.get
            (doc.rev must not beNull) and
            (doc.rev.length must beGreaterThan(0))
          }
        }
      }
    }
  }
  
  "getting an existing document by id/rev" should {
    "return a valid document id/rev" in {
      import CouchDatabaseSpec.JsonExamples._
      val res0 = orError(db.insert(shop1))
        res0 must beRight and {
        val doc1: CouchDoc = res0.right.get
        val res = orError(db.get(doc1.id))
        (res.left.toOption must beNone) and
        (res.right.toOption must beSome) and {
          val docOpt: Option[CouchDoc] = res.right.get
          (docOpt must beSome) and {
            val doc: CouchDoc = docOpt.get
            (doc.rev must not beNull) and
            (doc.rev.length must beGreaterThan(0))
          }
        }
      }
    }
  }
  
  "getting an missing document by id" should {
    "return an empty result" in {
      import CouchDatabaseSpec.JsonExamples._
      val id = generateRandomId() + "_nosuchthing"
      val res = orError(db.get(id))
      (res must beRight) and {
        val docOpt = res.right.get
        docOpt must beNone
      }
    }
  }
  
  "getting info for an existing document by id" should {
    "return a valid info object" in {
      import CouchDatabaseSpec.JsonExamples._
      val res0 = orError(db.insert(shop1))
      res0 must beRight and {
        val doc1: CouchDoc = res0.right.get
        val ref = doc1.ref
        val res = await60 { db.head(CouchDoc.Id(ref.id)) }
        res must beSome and {
          val headers = res.get
          (headers find { kv => kv._1 == "Content-Length" }) must beSome
        }
      }
    }
  }
  
  "clean up indexes" should {
    "return with no errors" in {
      val errorOpt = orError(db.viewCleanup)
      errorOpt must beRight
    }
  }
  
  "temporary views" should {
    
    import Json._
    
    val mapBody =
      "if (doc.type == 'myobj') emit([doc.name[0], doc.name[1]], doc.age);"
    val reduceBody = "return sum(values);"
    val valuePath = JsonPath.root / "value"
    
    def insertTestData() = {
      // create some data to work with
      db.insert(Json("type" -> "myobj", "name" -> "Jim", "age" -> 23))
      db.insert(Json("type" -> "myobj", "name" -> "Mary", "age" -> 34))
      db.insert(Json("type" -> "myobj", "name" -> "Jemima", "age" -> 45))
      db.insert(Json("type" -> "OTHER", "name" -> "Peter"))
      db.insert(Json("name" -> "Nancy"))
    }
    
    "do a basic map correctly" in {
      insertTestData()
      val res1 = orError(db.query(mapBody))
      val valuePath = JsonPath.root / "value"
      res1 must beRight and {
        val results = res1.right.get.toList
        results.length must beEqualTo(3)
        val ages =
          results.flatMap(_.getAt[JsonInteger](valuePath).map(_.value.toInt))
        ages.toSet must beEqualTo(Set(23, 34, 45))
      }
    }
    "do a basic reduce correctly" in {
      insertTestData()
      val res1 = orError(db.query(mapBody, Some(reduceBody)))
      res1 must beRight and {
        val results = res1.right.get.toList
        results.length must beEqualTo(1)
        val totalAge =
          results.flatMap(_.getAt[JsonInteger](valuePath).map(_.value.toInt))
        totalAge.toSet must beEqualTo(Set(102))
      }
    }
    "do a grouped reduce correctly" in {
      insertTestData()
      val res1 = orError(db.query(
        mapBody, Some(reduceBody), CouchViewOptions().groupLevel(1)
      ))
      res1 must beRight and {
        val results = res1.right.get.toList
        results.length must beEqualTo(2)
        val agesTotals =
          results.flatMap(_.getAt[JsonInteger](valuePath).map(_.value.toInt))
        agesTotals.toSet must beEqualTo(Set(68, 34))
      }
    }
    "add the correct options to the query string" in {
      // we'll just test this by converting the options to a map, since
      // the rest of the implementation is due to Url's query encoding
      val options = CouchViewOptions().
        key(JsonString("x")).
        keys(Json("x", "y")).
        startKey(Json("a", "b")).
        startKeyDocId("id123").
        endKey(Json("c", "d")).
        endKeyDocId("id456").
        limit(23).
        stale(CouchViewOptions.Stale.OK).
        descending(true).
        skip(97).
        group(true).
        groupLevel(34).
        reduce(true).
        includeDocs(true).
        inclusiveEnd(true).
        updateSeq(true)
      
      val optionsMap = options.toMap
      
      (optionsMap.get("key") must beSome(""""x"""")) and
      (optionsMap.get("keys") must beSome("""["x","y"]""")) and
      (optionsMap.get("startkey") must beSome("""["a","b"]""")) and
      (optionsMap.get("startkey_docid") must beSome("id123")) and
      (optionsMap.get("endkey") must beSome("""["c","d"]""")) and
      (optionsMap.get("endkey_docid") must beSome("id456")) and
      (optionsMap.get("limit") must beSome("23")) and
      (optionsMap.get("stale") must beSome("ok")) and
      (optionsMap.get("descending") must beSome("true")) and
      (optionsMap.get("skip") must beSome("97")) and
      (optionsMap.get("group") must beSome("true")) and
      (optionsMap.get("group_level") must beSome("34")) and
      (optionsMap.get("reduce") must beSome("true")) and
      (optionsMap.get("include_docs") must beSome("true")) and
      (optionsMap.get("inclusive_end") must beSome("true")) and
      (optionsMap.get("update_seq") must beSome("true")) and {
        
        val options2 = options.
          stale(CouchViewOptions.Stale.UPDATE_AFTER).
          descending(false).
          reduce(false).
          includeDocs(false).
          inclusiveEnd(false).
          updateSeq(false)
        
        val optionsMap2 = options2.toMap
        
        (optionsMap2.get("stale") must beSome("update_after")) and
        (optionsMap2.get("reduce") must beSome("false")) and
        (optionsMap2.get("descending") must beSome("false")) and
        (optionsMap2.get("include_docs") must beSome("false")) and
        (optionsMap2.get("inclusive_end") must beSome("false")) and
        (optionsMap2.get("update_seq") must beSome("false"))
      }
    }
  }
  
  "revision limit" should {
    "be settable and gettable" in {
      val res1 = orError(db.revisionLimit(6578))
      res1 must beRight and {
        val res2 = orError(db.revisionLimit())
        res2 must beRight and {
          val lim: Int = res2.right.get
          lim must beEqualTo(6578)
        }
      }
    }
  }
  
  "compact" should {
    "return with no errors" in {
      val errorOpt = orError(db.compact())
      errorOpt must beRight
    }
  }
  
  "ensure full commit" should {
    "return with no errors" in {
      val errorOpt = orError(db.ensureFullCommit())
      errorOpt must beRight
    }
  }
  
  "bulk docs" should {
    import CouchDoc.Rev
    "allow some docs with no IDs to be inserted" in {
      import Json._
      val docs = List(
        (None, Json("locality" -> "Moana")),
        (None, Json("locality" -> "Mount Gambier")),
        (None, Json("locality" -> "Willunga"))
      )
      val res1 = orError(db.bulkDocs(docs)) ; Thread.sleep(5000)
      res1 must beRight and {
        val results: Seq[Try[Rev]] = res1.right.get
        (results.size must beEqualTo(3)) and {
          val entries = results collect { case Success(ref: Rev) => ref }
          entries.size must beEqualTo(3)
        }
      }
    }
    
    "allow some docs with IDs to be inserted and then updated" in {
      import Json._
      import CouchDoc._
      val docs = List(
        (Some(Id("locality1")), Json("locality" -> "Moana")),
        (Some(Id("locality2")), Json("locality" -> "Mount Gambier")),
        (Some(Id("locality3")), Json("locality" -> "Willunga"))
      )
      val res1 = orError(db.bulkDocs(docs))
      (res1 must beRight) and {
        val results: Seq[Try[Rev]] = res1.right.get
        (results.size must beEqualTo(3)) and {
          val entries = results collect { case Success(ref: Rev) => ref };
          val ids = entries.map(_.id).map(JsonString(_));
          val idSet = ids.map(_.value).toSet
          val queryOpts = CouchViewOptions().includeDocs(true).keys(Json(ids:_*));
          (idSet must haveTheSameElementsAs(docs.map(_._1.get.id).toSet)) and {
            val res2 = orError(db.allDocs(queryOpts));
            (res2 must beRight) and {
              val results1 = res2.right.get.toList
              (results1 must haveSize(3)) and {
                val docs2Json = results1.flatMap(_.getAt[JsonObject]("doc"));
                val docs3 = docs2Json map { jobj: JsonObject =>
                  (
                    Some(Rev(
                      jobj.getAt[JsonString]("_id").get.value,
                      jobj.getAt[JsonString]("_rev").get.value
                    )),
                    (jobj ++ Json("locality" -> "Other")) : JsonObject
                  )
                };
                (docs3 must haveSize(3)) and {
                  val res3 = orError(db.bulkDocs(docs3))
                  (res3 must beRight) and {
                    val res4 = orError(db.allDocs(queryOpts))
                    (res4 must beRight) and {
                      val docs4 = res4.right.get.flatMap(
                        _.getAt[JsonObject]("doc")
                      ).toList;
                      (docs4 must haveSize(3)) and {
                        val localities  =
                          docs4.flatMap(_.getAt[JsonString]("locality")).map(_.value);
                        (localities must haveSize(3)) and {
                          localities.toSet must
                            haveTheSameElementsAs(List("Other"))
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
  
  "all docs" should {
    "retreive all docs" in {
      import Json._
      val docs1 = List(
        ("locality1", Json("locality" -> "Moana")),
        ("locality2", Json("locality" -> "Mount Gambier")),
        ("locality3", Json("locality" -> "Willunga"))
      )
      val localities1 = docs1.flatMap(_._2.getAt[JsonString]("locality"))
      assert(localities1.size == 3, "locality data check")
      val insertResults = docs1 map { case (id, jdoc) => orError(db.insert(id, jdoc)) }
      val insertFailed: Boolean = insertResults.find(_.isLeft).isDefined
      (insertFailed must beFalse) and {
        val docs2Res = orError(db.allDocs(CouchViewOptions().includeDocs(true)))
        (docs2Res must beRight) and {
          val docs2 = docs2Res.right.get.toList.flatMap(_.getAt[JsonObject]("doc"))
          docs2 must haveSize(3) and {
            val localities2 = docs2.flatMap(_.getAt[JsonString]("locality"))
            (localities1 must haveTheSameElementsAs(localities2))
          }
        }
      }
    }
    // Note that the use of all of the options in CouchViewOptions is 
    // thoroughly checked in the tests for temporary views, so we don't
    // need to repeat this here
  }
  
  "all docs (POST version)" should {
    "retreive all docs" in {
      import Json._
      val docs1 = List(
        ("locality1", Json("locality" -> "Moana")),
        ("locality2", Json("locality" -> "Mount Gambier")),
        ("locality3", Json("locality" -> "Willunga"))
      )
      val localities1 = docs1.flatMap(_._2.getAt[JsonString]("locality"))
      val ids = docs1.map(_._1)
      assert(localities1.size == 3, "locality data check")
      val insertResults = docs1 map { case (id, jdoc) => orError(db.insert(id, jdoc)) }
      val insertFailed: Boolean = insertResults.find(_.isLeft).isDefined
      (insertFailed must beFalse) and {
        val docs2Res = orError(db.allDocsPost(ids, CouchViewOptions().includeDocs(true)))
        (docs2Res must beRight) and {
          val docs2 = docs2Res.right.get.toList.flatMap(_.getAt[JsonObject]("doc"))
          docs2 must haveSize(3) and {
            val localities2 = docs2.flatMap(_.getAt[JsonString]("locality"))
            (localities1 must haveTheSameElementsAs(localities2))
          }
        }
      }
    }
  }
  
  "changes" should {
    "show added docs" in {
      val changes1Res = orError(db.changes())
      changes1Res must beRight and {
        val changes1: CouchResultIterator = changes1Res.right.get
        changes1.nextResult must beNone and {
          orError(db.insert(JsonObject())).right.get
          orError(db.insert(JsonObject())).right.get
          orError(db.insert(JsonObject())).right.get
          val changes2Res = orError(db.changes())
          changes2Res must beRight and {
            val changes2 = changes2Res.right.get.toList
            changes2.size must beEqualTo(3) and {
              ((changes2.head).get("seq") must beSome) and
                ((changes2.tail.head).get("seq") must beSome) and
                ((changes2.tail.tail.head).get("seq") must beSome)
            }
          }
        }
      }
    }
    "add the correct options to the query string" in {
      // we'll just test this by converting the options to a map, since
      // the rest of the implementation is due to Url's query encoding
      val design = db.design("fake")
      val options = CouchChangeOptions().since(123).limit(456).
        descending(true).heartbeat(7899).timeout(9876).
        filter(CouchFilter(design, "foo")).includeDocs(false).
        style(CouchChangeOptions.Style.ALL_DOCS)
      val optionsMap = options.toMap
      (optionsMap.get("since") must beSome("123")) and
      (optionsMap.get("limit") must beSome("456")) and
      (optionsMap.get("descending") must beSome("true")) and
      (optionsMap.get("heartbeat") must beSome("7899")) and
      (optionsMap.get("timeout") must beSome("9876")) and
      (optionsMap.get("filter") must beSome("fake/foo")) and
      (optionsMap.get("include_docs") must beSome("false")) and
      (optionsMap.get("style") must beSome("all_docs")) and {
        val options2 = options.descending(false).includeDocs(true).
          style(CouchChangeOptions.Style.MAIN_ONLY)
        val optionsMap2 = options2.toMap
        (optionsMap2.get("descending") must beSome("false")) and
        (optionsMap2.get("include_docs") must beSome("true")) and
        (optionsMap2.get("style") must beSome("main_only"))
      }
    }
  }
}
