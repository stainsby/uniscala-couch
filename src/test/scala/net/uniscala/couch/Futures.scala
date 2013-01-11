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

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


object Futures {
  
  import scala.language.postfixOps
  
  def awaitFor[T](dur: Duration)(f: Future[T]): T = Await.result[T](f, dur)
  
  def await[T](f: Future[T]): T = awaitFor[T](Duration.Inf)(f)
  
  def await5[T](f: Future[T]): T = awaitFor[T](5 seconds)(f)
  
  def await10[T](f: Future[T]): T = awaitFor[T](10 seconds)(f)
  
  def await30[T](f: Future[T]): T = awaitFor[T](30 seconds)(f)
  
  def await60[T](f: Future[T]): T = awaitFor[T](60 seconds)(f)
  
  def await120[T](f: Future[T]): T = awaitFor[T](120 seconds)(f)
  
  def await300[T](f: Future[T]): T = awaitFor[T](300 seconds)(f)
  
  def asTry[T](f: Future[T]): Try[T] = {
    try {
      await60 { f }
    } catch {
      case t: Throwable => // do nothing
    }
    f.value.get
  }
  
  def orError[T](fut: Future[T]): Either[Throwable, T] = {
    asTry(fut) match {
      case Success(v) => Right(v)
      case Failure(e) => Left(e)
    }
  }
  
  def sleep(t: Long) {
    println("SLEEPING " + t/1000.0 + " seconds ..")
    Thread.sleep(t)
    println(" .. WAKING")
  }
}
