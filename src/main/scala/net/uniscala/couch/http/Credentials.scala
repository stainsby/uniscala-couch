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
package net.uniscala.couch.http


sealed abstract class Credentials


case class BasicCredentials(userId: String, password: String)
extends Credentials {
  lazy val authorizationHeader = {
    val credStr = userId + ":" + password
    "Basic " + (new sun.misc.BASE64Encoder()).encode(credStr.getBytes("utf8"))
  }
}
