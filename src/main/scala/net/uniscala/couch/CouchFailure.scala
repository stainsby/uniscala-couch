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
 * Represents a failure that was returned as a JSON object from a couch 
 * server. Such failures have an 'error' field and (optionally?) a
 * 'reason' field, which are converted here into the 'error' and 
 * 'reasonOption' members.
 */
case class CouchFailure(error: String, reasonOption: Option[String])
extends Throwable(error + reasonOption.map(": " + _).getOrElse(""))
