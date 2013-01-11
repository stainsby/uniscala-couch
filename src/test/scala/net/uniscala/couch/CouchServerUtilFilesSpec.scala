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

import java.io._

import org.specs2.mutable._
import org.specs2.specification.Scope
import org.specs2.specification.Outside

import net.uniscala.json._

import Futures._


class CouchServerUtilFilesSpec extends Specification {
  
  sequential
  
  lazy val couch = CouchClientSpec.couch
  lazy val files = couch.utilFiles
  
  "server utils files" should {
    "access some well-known files" in {
      val res1 = orError(files.get("index.html"))
      val res2 = orError(files.get("style/layout.css"))
      (res1 must beRight) and (res2 must beRight) and {
        val file1Opt = res1.right.get
        val file2Opt = res2.right.get
        (file1Opt must beSome) and (file2Opt must beSome) and {
          val file1 = file1Opt.get
          val file2 = file2Opt.get
          (file1.contentType must beEqualTo("text/html")) and
          (file2.contentType must beEqualTo("text/css")) and
          (file1.lengthOption must beSome) and
          (file2.lengthOption must beSome) and {
            val length1: Long = file1.lengthOption.get
            val length2: Long = file1.lengthOption.get
            (length1 must beGreaterThan(0L)) and
            (length2 must beGreaterThan(0L)) and {
              val r = scala.io.Source.fromInputStream(file1.stream)
              val content = r.getLines.mkString("\n")
              r.close
              content must contain("<!DOCTYPE html>")
            }
          }
          
        }
      }
    }
  }
}
