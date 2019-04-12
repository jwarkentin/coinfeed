package coinfeed.connectors

import coinfeed.TestBase
import kotlin.test.Test

class CoinbaseSourceTest : TestBase() {
  @Test fun testThing() {
    val kafka = getKafka()
    kafka.startup()
    val connect = getKafkaConnect()
    connect.createConnector(mapOf())
  }
}
