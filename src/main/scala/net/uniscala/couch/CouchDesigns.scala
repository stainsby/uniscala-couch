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

import net.uniscala.json.JsonObject


/**
 * Represents a base for resources found below "/_design/" in a database.
 */
class CouchDesigns private[couch] (val database: CouchDatabase)
extends CouchSubPath(database, CouchDatabase.Path.DESIGN)
with CouchPathContainerOps[CouchDoc] {
  
  override def newDoc(id: String, rev: String, jdoc: JsonObject) =
    CouchDoc(database, id, rev, jdoc)
}
