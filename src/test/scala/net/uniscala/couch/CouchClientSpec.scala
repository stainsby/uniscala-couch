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

import scala.concurrent.Await
import scala.concurrent.duration._

import java.lang.Thread.sleep

import org.specs2.mutable._
import org.specs2.specification.Scope
import org.specs2.specification.Outside

import net.uniscala.json._

import http.BasicCredentials
import util.Url

import Futures._


object CouchClientSpec {
  
  import scala.concurrent.ExecutionContext.Implicits.global
  
  @volatile var couchOpened = false
  
  lazy val couch: CouchClient = {
    val creds = BasicCredentials("admin", "changeme")
    val c = new CouchClient(credentialsOption = Some(creds))
    couchOpened = true
    c
  }
  
  scala.sys.addShutdownHook({
    if (couchOpened) {
      couch.shutdown()
    }
  })
  
  def database(dbName: String) = couch.database(dbName)
  
  def deleteTestDatabase(name: String) = {
    sleep(100)
    val res = asTry(couch.deleteDatabase(name))
    sleep(300)
  }
  
  def renewTestDatabase(name: String) = {
    deleteTestDatabase(name)
    val res = asTry(couch.createDatabase(name))
    sleep(300)
  }
  
  class Cleanups(dbName: String) extends BeforeAfter {
    
    lazy val couch = CouchClientSpec.couch
    
    override def before() = renewTestDatabase(dbName)
    
    override def after() = deleteTestDatabase(dbName)
  }
  
  def generateRandomId(length: Int = 16): String = {
    import scala.math._
    val allowedChars = "abcdefghijklmnopqrstuvwxyz"
    val n = allowedChars.length
    val timestamp = System.currentTimeMillis.toString
    val ss: Seq[Char] = for (i <- Range(0, length)) yield
      allowedChars(round((random*(n - 1)).toFloat))
    ss.mkString + timestamp
  }
}


class CouchClientSpec extends Specification {
  
  import Couch._
  
  sequential
  
  val dbName = "uniscala_couch_client_test"
  implicit lazy val myContext = new CouchClientSpec.Cleanups(dbName)
  lazy val couch = CouchClientSpec.couch
  lazy val db: CouchDatabase = CouchClientSpec.database(dbName)
  
  def checkDatabaseLifecycle(dbNameSuffix: String) = {
    val name = dbName + "_" + dbNameSuffix
    couch.deleteDatabase(name)
    val res = orError(couch.createDatabase(name))
    (res must beRight) and
    (res.right.get.id mustEqual name) and
    (orError(couch.deleteDatabase(name)) must beRight)
  }
  
  "info method" should {
    "return some server info" in {
      val res = orError(couch.info())
      (res must beRight) and {
        val info = res.right.get
        (info must not beNull) and {
          val keys = info.keys
          (keys must contain("couchdb")) and (keys must contain("version"))
        }
      }
    }
  }
  
  "favicon method" should {
    "return some bytes" in {
      val res  = orError(couch.favicon)
      res must beRight and {
        val bytes: Array[Byte] = res.right.get
        bytes.size must beGreaterThan(0)
      }
    }
  }
  
  "databaseNames method" should {
    "return database names that are always present" in {
      val res = orError(couch.databaseNames)
      (res must beRight) and {
        val names: Seq[Any] = res.right.get.map(_.value)
        (names must not beNull) and {
          (names must contain("_replicator")) and
            (names must contain("_users"))
        }
      }
    }
  }
  
  "activeTasks method" should {
    "return a JSON array" in {
      val res = orError(couch.activeTasks)
      (res must beRight)
    }
  }
  
  "generateUuids method" should {
    "without args get one UUID" in {
      val res = orError(couch.generateUuids())
      (res must beRight) and {
        val jobj = res.right.get
        val uuidsOpt = jobj.getAt[JsonArray]("uuids")
        uuidsOpt must beSome and {
          val uuids = uuidsOpt.get.map(_.value)
          (uuids.size must beEqualTo(1)) and
            (uuids(0) must not beNull)
        }
      }
    }
    "with args get some UUIDs" in {
      val res = orError(couch.generateUuids(3))
      (res must beRight) and {
        val jobj = res.right.get
        val uuidsOpt = jobj.getAt[JsonArray]("uuids")
        uuidsOpt must beSome and {
          val uuids = uuidsOpt.get.map(_.value)
          (uuids.size must beEqualTo(3)) and
            (uuids(0) must not beNull) and
            (uuids(1) must not beNull) and
            (uuids(2) must not beNull)
        }
      }
    }
  }
  
  "stats method" should {
    "return a JSON object with stats" in {
      val res = orError(couch.stats())
      (res must beRight) and {
        val stats: JsonObject = res.right.get
        val keys = stats.keys
        (keys must contain("couchdb")) and
          (keys must contain("httpd"))
      }
    }
  }
  
  "log method" should {
    "return some log data" in {
      val res = orError(couch.log())
      (res must beRight) and {
        val logData = res.right.get
        logData.length must beGreaterThan(0) and {
          val res2 = orError(couch.log(bytesOption=Some(10)))
          (res2 must beRight) and {
            val logData2: String = res2.right.get;
            (logData2.length must beEqualTo(10)) and {
              val res3 = orError(couch.log(offsetOption=Some(13)))
              (res3 must beRight) and {
                val logData3 = res3.right.get
                // can't think of a good way to test that this parameter is used
                logData3.length must beGreaterThan(0)
              }
            }
          }
        }
      }
    }
  }
  
  /* DISABED BECAUSE THIS CAUSES PROBLEMS WITH OTHER SPECS
  "restart method" should {
    "clear the stats" in {
      val res = client.restart()
      println("restarting - sleeping for a while to allow this to happen ...")
      Thread.sleep(40*1000)
      println("... continuing")
      (res must beNone) and {
        def requests() = client.stats().getAt[JsonFloat](
          JsonPath.root / "httpd" / "requests" / "current"
        )
        val reqs1 = requests()
        (reqs1 must beNone)
        Thread.sleep(1*1000) // allow time for stats to update
        val reqs2 = requests()
        (reqs2 must beSome) and (reqs2.get.value must beEqualTo(1.0D))
      }
    }
  }
  */
  
  "creating and deleting a database with a simple name" should {
    "have no errors" in {
      checkDatabaseLifecycle("x")
    }
  }
  
  // check legal database name chars (not as first char though): $()+-/
  
  "creating and deleting a database with a '$' name character" should {
    "have no errors" in {
      checkDatabaseLifecycle("x$x")
    }
  }
  "creating and deleting a database with a '(' name character" should {
    "have no errors" in {
      checkDatabaseLifecycle("x(x")
    }
  }
  "creating and deleting a database with a ')' name character" should {
    "have no errors" in {
      checkDatabaseLifecycle("x)x")
    }
  }
  // it appears that couch doesn't actually support '+' in the database
  // name, despite the spec @ http://wiki.apache.org/couchdb/HTTP_database_API.
  // We've reported the issue here:
  //   https://issues.apache.org/jira/browse/COUCHDB-1580
  // 
  "creating and deleting a database with a '+' name character" should {
    "have no errors" in {
      checkDatabaseLifecycle("x+x")
    }.pendingUntilFixed(".. https://issues.apache.org/jira/browse/COUCHDB-1580")
  }
  
  "creating and deleting a database with a '-' name character" should {
    "have no errors" in {
      checkDatabaseLifecycle("x-x")
    }
  }
  "creating and deleting a database with a '/' name character" should {
    "have no errors" in {
      checkDatabaseLifecycle("x/x")
    }
  }
}