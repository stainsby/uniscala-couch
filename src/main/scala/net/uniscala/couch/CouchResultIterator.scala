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

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

import java.io.Reader

import io.netty.handler.codec.http.HttpRequest

import net.uniscala.json._

import net.uniscala.couch.http.Response
import CouchView.Field


private[couch] object CouchResultIterator {
  
  import ExecutionContext.Implicits.global
  
  private[couch] def apply(
    client: CouchClient,
    req: HttpRequest,
    resultsKey: String = Field.ROWS
  ): Future[CouchResultIterator] = {
    client.send(req) flatMap {
      _.content.withReader((r) => new CouchResultIterator(r, resultsKey))
    }
  }
}


/**
 * Used for results from queries that adhere to the couch view API.
 * The rows of the view result are returned as JSON objects through this
 * iterator.
 */
class CouchResultIterator private[couch] (
  reader: Reader,
  resultsKey: String = Field.ROWS)
extends JsonParser(reader) with Iterator[JsonObject] {
  
  private var resultBuffer: Option[JsonObject] = None // buffer for one result
  private var atEnd = false
  
  init()
  
  private def init(): Unit = {
    
    // skip preamble info like "total_rows" etc. (or we could use these!)
    @tailrec def preamble(): Unit = {
      skipWhitespace
      val key: String = string;
      if (key != resultsKey) {
        skipKeyValue
        preamble
      }
    }
    
    skipWhitespace
    consumeChar('{')
    preamble
    skipWhitespace
    consumeChar(':')
    skipWhitespace
    consumeChar('[')
    skipWhitespace
    if (currentChar == ']') { // empty results
      atEnd = true
      try { close }
    }
  }
  
  def skipKeyValue(): Unit = {
    skipWhitespace
    consumeChar(':')
    val skipped = jvalue
    skipWhitespace
    consumeChar(',')
  }
  
  def nextResult(): Option[JsonObject] = if (hasNext) Some(next) else None
  
  override def hasNext() = {
    if (atEnd) {
      false
    } else {
      resultBuffer.isDefined || {
        resultBuffer = parseNextResult
        resultBuffer.isDefined
      }
    }
  }
  
  override def next() = {
    val resultOpt: Option[JsonObject] = resultBuffer orElse {
      resultBuffer = parseNextResult
      resultBuffer
    }
    resultBuffer = None
    resultOpt getOrElse {
      throw new RuntimeException("no more results to read")
    }
  }
  
  def parseNextResult(): Option[JsonObject] = {
    if (atEnd) {
      None
    } else {
      val row = parseObject
      advance
      skipWhitespace
      if (currentChar != ',') {
        atEnd = true
      } else {
        advance
      }
      Some(row)
    }
  }
  
  def close: Unit = reader.close
}