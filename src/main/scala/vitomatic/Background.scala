package vitomatic

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, Event, MessageEvent, XMLHttpRequest}
import org.scalajs.dom.raw.WebSocket
import dom.ext.{Ajax, AjaxException}
import org.scalajs.dom.ext.Ajax.InputData

import scala.concurrent.ExecutionContext.Implicits.global
import js.Dynamic.{global => g}
import upickle.default._

import scala.compat.Platform
import scala.scalajs.js.URIUtils._
import scala.scalajs.js.{Date, JSON}

object Background extends js.JSApp {

  def subscribe(agentDetail: AgentDetail) = {

    var timerId: Int = 0

    val encodedAgentDetail = encodeURIComponent(write(agentDetail))
    debug(s"Encoded agent detail: ${encodedAgentDetail}")

    def endpoint(path: String, protocol: String = "http"): String = {
      val host = "hackathon.tailrec.io"
//      val host = "localhost"
      val ep = s"${protocol}://${host}:9000/${path.stripPrefix("/")}"
      ep
    }

    def createSocket(): Unit = {
      debug("Creating new socket")
      val socket = new WebSocket(endpoint(s"/agent/subscribe/${encodedAgentDetail}", "ws"))
      socket.onopen = { (event: Event) =>
        if(timerId != 0) {
          dom.window.clearInterval(timerId)
          timerId = 0
        }
      }
      socket.onclose = { (event: CloseEvent) =>
        if(timerId == 0)
        timerId = dom.window.setInterval(() => createSocket(), 5000)
      }
      socket.onmessage = { (event: MessageEvent) =>
        val data = event.data.asInstanceOf[String]
        val config = read[LoadTestConfig](data)

        dom.window.alert(s"Let's attack: ${config.targetUrl}")

        (0 until config.loopCount).foreach { i =>
          val startTime = Platform.currentTime
          val xhr = new XMLHttpRequest()
          Ajax(
            method = config.method.name,
            url = config.targetUrl,
            data = InputData.str2ajax(config.body.getOrElse("")), //support only JSON content type for now
            timeout = 0,
            headers = config.headers.foldLeft(Map.empty[String, String]){ (m, h) => m + (h.name -> h.value)},
            withCredentials = false,
            responseType = ""
          ).map { res =>
            val rtt = Platform.currentTime - startTime
            val result = write(SingleHitResult(config.clientId, agentDetail.address, config.targetUrl, res.status, true, rtt, res.statusText))
            enqueue(result)
          }.onFailure {
            case AjaxException(res) =>
              val rtt = Platform.currentTime - startTime
              val result = write(SingleHitResult(config.clientId, agentDetail.address, config.targetUrl, res.status, false, rtt, res.statusText))
              enqueue(result)
          }
        }
      }
    }
    createSocket()
  }

  def enqueue(data: String) = {
    //API key not require for the sake of hackathon!
    val enqueueUrl = "https://k6xiqf7156.execute-api.us-east-1.amazonaws.com/prod/EnqueueLoadHitResult"
    Ajax.post(enqueueUrl, data)
  }

  def main(): Unit = {

    dom.window.alert("Welcome to the army! I'll let you know when we move.")

    Ajax.get("http://api.ipify.org/").onSuccess { case xhr =>
      val address = xhr.responseText
      val userAgent = dom.window.navigator.userAgent
      val chromeVersion = userAgent.substring(userAgent.indexOf("Chrome/") + 7, userAgent.lastIndexOf(" "))

      val timezone = g.Intl.DateTimeFormat().resolvedOptions().timeZone.asInstanceOf[String]
      val tzOffset = new Date().getTimezoneOffset() / -60
      val agentDetail = AgentDetail(address, userAgent, AgentTimeZone(timezone, tzOffset))
      debug(s"Agent detail: ${agentDetail}")
      subscribe(agentDetail)
    }
  }

  def debug(message: String): Unit = {
    g.chrome.extension.getBackgroundPage().console.log(message)
  }

}
