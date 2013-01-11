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

import scala.concurrent.Future

import net.uniscala.json._

import util.{Mime, Url}


private[couch] object CouchServerUtilFiles {
  
  private[couch] object Path {
    val UTIL: String = "_utils"
  }
}



/**
 * A path for server utils file operations.
 */
class CouchServerUtilFiles private[couch] (val couch: CouchClient)
extends CouchSubPath(couch, CouchServerUtilFiles.Path.UTIL) {
  
  
  // API methods
  
  
  /**
   * Gets the utils file specified by the file name.
   */
  def get(fileName: String): Future[Option[CouchAttachment]] =
    CouchAttachment.atPath(this, fileName)
}
