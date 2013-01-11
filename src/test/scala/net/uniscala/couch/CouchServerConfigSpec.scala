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


class CouchServerConfigSpec extends Specification {
  
  sequential
  
  lazy val couch = CouchClientSpec.couch
  lazy val config = couch.config
  
  
  "the server config" should {
    "contain some well-known keys" in {
      val res1 = orError(config.get())
      res1 must beRight and {
        val cfg = res1.right.get
        (cfg.get("log") must beSome) and
        (cfg.get("httpd") must beSome) and
        (cfg.get("uuids") must beSome)
      }
    }
    "allow sections to be retrieved" in {
      val res1 = orError(config.get("log"))
      res1 must beRight and {
        val cfg = res1.right.get
        (cfg.get("file") must beSome) and
        (cfg.get("level") must beSome)
      }
    }
    "allow values to be sets and deleted" in {
      val res1 = orError(config.set("log", "testing123", JsonString("frodo")))
      res1 must beRight and {
        val res2 = orError(config.get("log"))
        res2 must beRight and {
          val cfg = res2.right.get
          cfg.get("testing123") must beSome and {
            val v = cfg.getAt[JsonString]("testing123").map(_.value)
            v must beSome("frodo")
            val res3 = orError(config.delete("log", "testing123"))
            res3 must beRight and {
              val res4 = orError(config.get("log"))
              res4 must beRight and {
                val cfg = res4.right.get
                cfg.get("testing123") must beNone
              }
            }
          }
        }
      }
    }
  }
}
