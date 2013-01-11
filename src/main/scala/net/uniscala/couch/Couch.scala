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

import net.uniscala.json._

import http._
import util.Url


object Couch {
  
  private[couch] object Field {
    val ERROR = "error"
    val NOT_FOUND = "not_found"
  }
  
  private[couch] def enc(url: Url): String = url.encodePathOnwards
  
  private[couch] def jsonStringValue(json: JsonValue[_]): String = json match {
    case JsonString(s) => s
    case _ => json.toString
  }
  
  private[couch] def toCouchFailure(jobj: JsonObject): CouchFailure = {
    val err = jobj.get("error")
    assert(err.isDefined, "JSON object is not a couch error")
    val reasonOpt = jobj.get("reason").map(jsonStringValue(_))
    new CouchFailure(jsonStringValue(err.get), reasonOpt)
  }
  
  private[couch] def toCouchFailure(
    statusCode: Int,
    statusMessageOption: Option[String] = None
  ): CouchFailure = {
    val msg: String = statusMessageOption getOrElse {
      statusCode match {
        case 400 => "bad request"
        case 401 => "not authorized"
        case 404 => "object not found, or no such URL"
        case 405 => "URL doesn't exist"
        case 409 => "conflicts"
        case 412 => "already exists"
        case 500 => "invalid JSON, or system error"
        case _ => "unhandled HTTP error"
      }
    }
    CouchFailure(msg, Some("[HTTP status: " + statusCode + "]"))
  }
  
  private[couch] val toJson: String => JsonTop[_] = JsonParser.parseTop(_)
  
  private[couch] val toJsonObject: String => JsonObject =
    JsonParser.parseObject(_)
  
  private[couch] val toJsonArray: String => JsonArray =
    JsonParser.parseArray(_)
  
  /**
   * If this supplied JSON object is and error (contains and 'error'
   * field), then throws an approriate CouchFaiure exception. Otherwise,
   * the JSON boejct is passed through unmodifed.
   */
  private[couch] val trapFailure: JsonObject => JsonObject = { jobj =>
    jobj.get("error") map { errorJson =>
      val reasonOpt = jobj.get("reason").map(_.toString)
      throw toCouchFailure(jobj)
    }
    jobj
  }
  
  /**
   * Converts JSON text (typically a database reply) into a JSON object, 
   * trapping any errors. If the resulting object represents
   * an error message from the database (if it contains and 'error'
   * field), then throw an appropriate CouchFailure exception. Otherwise,
   * return the JSON object.
   */
  val toSafeJsonObject: String => JsonObject =
    toJsonObject andThen trapFailure
  
  /**
   * Converts JSON text (typically a database reply)  into a JSON array, 
   * trapping any errors. If the JSON text represents an error message from 
   * the database (if it is a JSON obejct that contains and 'error' field), 
   * then throw an appropriate CouchFailure exception. Otherwise, return 
   * the JSON array.
   */
  val toSafeJsonArray: String => JsonArray = toJson andThen {
    _ match {
      case jarr: JsonArray => jarr
      case err: JsonObject => {
        trapFailure(err) // this should throw
        throw new RuntimeException("illegal couch reply")
      }
      case _ => {
        throw new RuntimeException("illegal couch reply")
      }
    }
  }
}
