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

  def subscribe(encodedAgentDetail: String) = {

    var timerId: Int = 0

    def endpoint(path: String, protocol: String = "http"): String = {
      //val host = "hackathon.tailrec.io"
      val host = "localhost"
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
            debug("Success")
            //TODO: send result to SQS
            val rtt = Platform.currentTime - startTime
            val result = write(SingleHitResult(config.clientId, config.targetUrl, res.status, true, rtt, res.statusText))
            Ajax.post(endpoint("/sqs"), result)
          }.onFailure {
            case AjaxException(res) =>
              val rtt = Platform.currentTime - startTime
              val result = write(SingleHitResult(config.clientId, config.targetUrl, res.status, false, rtt, res.statusText))
              Ajax.post(endpoint("/sqs"), result)
          }
        }
      }
    }
    createSocket()
  }

  def main(): Unit = {

    debug("Welcome to the club!")

    Ajax.get("http://api.ipify.org/").onSuccess { case xhr =>
      val address = xhr.responseText
      val userAgent = dom.window.navigator.userAgent
      val chromeVersion = userAgent.substring(userAgent.indexOf("Chrome/") + 7, userAgent.lastIndexOf(" "))

      val timezone = g.Intl.DateTimeFormat().resolvedOptions().timeZone.asInstanceOf[String]
      val tzOffset = new Date().getTimezoneOffset() / -60
      val agentDetail = write(AgentDetail(address, userAgent, AgentTimeZone(timezone, tzOffset)))
      dom.console.log(s"Agent detail: ${agentDetail}")
      val encodedAgentDetail = encodeURIComponent(agentDetail)
      dom.console.log(s"Encoded agent detail: ${encodedAgentDetail}")
      subscribe(encodedAgentDetail)
    }
  }

  def debug(string: String): Unit = {
    dom.window.alert(string)
  }
}
