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

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


object Futures {
  
  /**
   * A convenient ay of awating a future. This simply calls:
   * Await.result[T](f, dur)
   */
  def awaitFor[T](dur: Duration)(f: Future[T]): T = Await.result[T](f, dur)
  
  /**
   * A convenient ay of awating a future. This simply calls:
   * awaitFor[T](Duration.Inf)(f)
   */
  def await[T](f: Future[T]): T = awaitFor[T](Duration.Inf)(f)
}
