package coinfeed.connectors

import coinfeed.TestBase
import coinfeed.utils.cfgToObj
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.fixedRateTimer
import kotlin.test.Test

class CoinbaseSourceTest : TestBase() {
  @Test fun testThing() {
    val config = cfgToObj(this.javaClass.getResourceAsStream("/config.yml"))!!

    val kafka = getKafka()
    kafka.startup()
    val connect = getKafkaConnect()
    connect.createConnector(mapOf(
      "name" to "coinbase-feed",
      "bootstrap.servers" to connect.workerConfig["bootstrap.servers"]!!,
      "connector.class" to "coinfeed.connectors.coinbase.CoinbaseSourceConnector",
      "cbproapi.auth.apiKey" to config["coinbase"]["auth"]["apiKey"].textValue(),
      "cbproapi.auth.apiSecret" to config["coinbase"]["auth"]["apiSecret"].textValue(),
      "cbproapi.auth.passphrase" to config["coinbase"]["auth"]["passphrase"].textValue(),
      "createMissingTopics" to config["createMissingTopics"].booleanValue().toString(),
      "topics" to config["topics"].toString()
    ))

//    GlobalScope.launch {
//      fixedRateTimer(daemon = true, period = 5000) {
//        logger.debug(kafka.readRecords("matches", 1))
//      }
//    }

    connect.connect.awaitStop()
  }
}
