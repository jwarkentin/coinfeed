package coinfeed.connectors

import coinfeed.TestBase
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlin.test.Test

class CoinbaseSourceTest : TestBase() {
  // fun readConfig(): Map<String, Any?> = Yaml().load(this.javaClass.getResourceAsStream("/config.yml"))
  private fun readConfig(): JsonNode? = ObjectMapper(YAMLFactory()).readTree(
    this.javaClass.getResourceAsStream("/config.yml")
  )

  @Test fun testThing() {
    val config = readConfig()!!

    val kafka = getKafka()
    kafka.startup()
    val connect = getKafkaConnect()
    connect.createConnector(mapOf(
      "name" to "coinbase-feed",
      "bootstrap.servers" to connect.workerConfig["bootstrap.servers"]!!,
      "connector.class" to "coinfeed.connectors.CoinbaseSourceConnector",
      "cbproapi.auth.apiKey" to config["coinbase"]["auth"]["apiKey"].textValue(),
      "cbproapi.auth.apiSecret" to config["coinbase"]["auth"]["apiSecret"].textValue(),
      "cbproapi.auth.passphrase" to config["coinbase"]["auth"]["passphrase"].textValue()
    ))
  }
}
