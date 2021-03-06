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


/**
 * Refers to a filter defined in a design.
 */
case class CouchFilter(design: CouchDesign, id: String) {
  override lazy val toString = design.id + "/" + id
}
