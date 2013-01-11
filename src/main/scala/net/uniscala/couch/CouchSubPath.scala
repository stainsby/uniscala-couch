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


/// TODO: abstract class CouchSubPath[P <: CouchPath](val parent: P, ... ?
/**
 * A non-root CouchPath (see CouchPath). The 'id' is the final 
 * path segment.  It is important to note that this is not
 * always the same as the document ID of a couch document that 
 * represents the associated resource in the database. For
 * example, in the case of a design document in couch that has an ID 
 * like '_design/aaa', the corresponding CocuhDesign object would
 * have an 'id' of 'aaa'.
 */
abstract class CouchSubPath(
  val parent: CouchPath,
  val id: String
) extends CouchPath {
  
  override val parentOption = Some(parent)
  
  override lazy val client: CouchClient = parent.client
  
  override lazy val baseUrl = parent.baseUrl / id
}
