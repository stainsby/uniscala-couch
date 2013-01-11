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

import io.netty.handler.codec.http.HttpRequest

import net.uniscala.json._


private[couch] object CouchServerConfig {
  
  private[couch] object Path {
    val CONFIG: String = "_config"
  }
}


/**
 * A path for server configuration operations.
 */
class CouchServerConfig private[couch] (val couch: CouchClient)
extends CouchSubPath(couch, CouchServerConfig.Path.CONFIG) {
  
  import scala.concurrent.ExecutionContext.Implicits.global
  
  import CouchServerConfig._
  import Couch._
    
  private def handleConfigResponse(req: HttpRequest) = {
    client.send(req) map { resp =>
      val statusCode = resp.statusCode 
      if (statusCode / 100 == 4) {
        throw Couch.toCouchFailure(statusCode, resp.statusMessageOption)
      }
    }
  }
  
  
  // API methods
  
  
  /**
   * API METHOD: Gets the full server configuration metadata.
   */
  def get(): Future[JsonObject] = fetchJsonObject(prepareGet())
  
  /**
   * API METHOD: Gets the server configuration metadata for a particular 
   * section.
   */
  def get(section: String): Future[JsonObject] =
    fetchJsonObject(prepareGet(baseUrl / section))
  
  /**
   * API METHOD: Modifies an item in the server configuration.
   */
  def set(section: String, key: String, value: JsonValue[_]): Future[Unit] =
    handleConfigResponse(preparePut(value, baseUrl / section / key))
  
  /**
   * API METHOD: Deletes an item in the server configuration.
   */
  def delete(section: String, key: String): Future[Unit] =
    handleConfigResponse(prepareDelete(baseUrl / section / key))
}
