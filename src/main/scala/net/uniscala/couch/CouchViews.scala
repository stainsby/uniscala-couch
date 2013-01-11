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
 * A base base for the couch views in a design document.
 */
class CouchViews private[couch] (val design: CouchDesign)
extends CouchSubPath(design, CouchDatabase.Path.VIEW)
