package coinfeed.connectors

import coinfeed.TestBase
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlin.test.Test

class CoinbaseSourceTest : TestBase() {
  // fun readConfig(): Map<String, Any?> = Yaml().load(this.javaClass.getResourceAsStream("/config.yml"))
  fun readConfig(): ObjectNode = ObjectMapper(YAMLFactory()).readTree(
    this.javaClass.getResourceAsStream("/config.yml")
  ) as ObjectNode

  @Test fun testThing() {
    val config = readConfig()
    // logger.info(config::class.java)
    // logger.info(config["coinbase"]["auth"]["apiKey"])

    val kafka = getKafka()
    kafka.startup()
    val connect = getKafkaConnect()
    connect.createConnector(mapOf(
      "name" to "coinbase-feed",
      "bootstrap.servers" to connect.workerConfig["bootstrap.servers"]!!,
      "connector.class" to "coinfeed.connectors.CoinbaseSourceConnector",
      "cbproapi.auth.apiKey" to config["coinbase"]["auth"]["apiKey"].toString(),
      "cbproapi.auth.apiSecret" to config["coinbase"]["auth"]["apiSecret"].toString()
    ))
  }
}
