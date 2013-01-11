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

import java.io.InterruptedIOException

import net.uniscala.json.JsonObject


/**
 * Pushes changes to the 'withChanges' function as they are read from the 
 * server's response to a continuous change request.
 *
 */
class CouchChangePusher(
  changes: CouchResultIterator,
  withChanges: JsonObject => _
) extends Runnable {
  
  protected var running = true
  protected var runner: Option[Thread] = None
  
  /**
   * Runs the pusher- used internally - clients should not call this method.
   */
  override def run = {
    this.runner = Some(Thread.currentThread)
    while (running) {
      try {
        if (changes.hasNext) {
          val change = changes.next
          withChanges(change)
        } else {
          running = false
        }
      } catch {
        case _: InterruptedException => // allow loop to continue
        case _: InterruptedIOException => // allow loop to continue
      }
    }
  }
  
  /**
   * Stops the pusher.
   */
  def finish: Unit = {
   running = false
   this.runner.foreach(_.interrupt)
   this.runner = None
  }
}