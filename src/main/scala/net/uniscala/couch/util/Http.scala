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
package net.uniscala.couch.util

object Http {
  
  object Header {
    val ACCEPT              = "Accept"
    val AUTHORIZATION       = "Authorization"
    val CONTENT_LENGTH      = "Content-Length"
    val CONTENT_TYPE        = "Content-Type"
    val COOKIE              = "Cookie"
    val DATE                = "Date"
    val DESTINATION         = "Destination"
    val ETAG                = "ETag"
    val HOST                = "Host"
    val SET_COOKIE          = "Set-Cookie"
    val USER_AGENT          = "User-Agent"
  }
  
  object Method {
    val GET       = "GET"
    val POST      = "POST"
    val PUT       = "PUT"
    val DELETE    = "DELETE"
    val HEAD      = "HEAD"
    val COPY      = "COPY"
  }
}
