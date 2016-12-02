package vitomatic

/**
  * @author Hussachai Puripunpinyo
  */
case class AgentDetail (
 address: String, //ip address of the node
 userAgent: String, //navigator.userAgent
 //To get Chrome version: userAgent.substring(userAgent.indexOf("Chrome/") + 7, userAgent.lastIndexOf(" "))
 timezone: AgentTimeZone
)

case class AgentTimeZone (
 name: String, //Intl.DateTimeFormat().resolvedOptions().timeZone
 offset: Int //new Date().getTimezoneOffset() / -60
)

case class LoadTestConfig (
  clientId: String,
  loopCount: Int,
  targetUrl: String,
  method: HttpMethod,
  headers: Seq[HttpHeader] = Nil,
  body: Option[String] = None
)

case class HttpMethod(name: String)

case class HttpHeader (
  name: String,
  value: String
) {
  override def toString(): String = s"$name=$value"
}

case class SingleHitResult (
  clientId: String,
  agentAddress: String,
  targetUrl: String,
  statusCode: Int,
  success: Boolean,
  totalTime: Long, // round trip time
  message: String
)