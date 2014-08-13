/*
 * Licensed to Typesafe under one or more contributor license agreements.
 * See the CONTRIBUTING file distributed with this work for
 * additional information regarding copyright ownership.
 * This file is licensed to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.squbs.testkit.util

import java.net.{DatagramSocket, ServerSocket}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object Ports {

  private[this] val unavailable = new ConcurrentHashMap[Int, Boolean]()
  private[this] val nextAttempt = new AtomicInteger(0)

  def available(lower:Int = 1000, upper:Int = 9999):Int = {

    lower + nextAttempt.getAndIncrement % (upper - lower) match {

      case verifying if !unavailable.containsKey(verifying) =>
        unavailable.put(verifying, true)
        //this logic is an from mina framework:
        //https://svn.apache.org/repos/asf/directory/sandbox/jvermillard/mina/java/org/apache/mina/util/AvailablePortFinder.java
        var serverSocket:Option[ServerSocket] = None
        var udpUseSocket:Option[DatagramSocket] = None
        try{
          serverSocket = Some(new ServerSocket(verifying))
          serverSocket.foreach(_.setReuseAddress(true))
          udpUseSocket = Some(new DatagramSocket(verifying))
          udpUseSocket.foreach(_.setReuseAddress(true))
          //success
          verifying
        }
        catch {
          case ex: Throwable => //failure, try next port
            available(lower, upper)
        }
        finally{
          try{
            udpUseSocket.foreach(_.close)
            serverSocket.foreach(_.close)
          }
          catch {
            case _: Throwable => //ignored
          }
        }
      //all ports in range are unavailable
      case _ if unavailable.size == upper - lower =>
        -1
      //further attempt with the next port
      case _ =>
        available(lower, upper)
    }
  }
}
