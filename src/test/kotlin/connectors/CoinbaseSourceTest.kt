package coinfeed.connectors

import coinfeed.TestBase
import coinfeed.connectors.coinbase.Schemas
import coinfeed.connectors.coinbase.models.receive.Match
import coinfeed.models.WindowSummary
import coinfeed.utils.cfgToObj
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
//import coinfeed.views.MatchSummary
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Initializer
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.TimeWindows
import java.time.Duration
import java.util.*
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

    logger.info("Zookeeper Port: ${kafka.zkPort}")
    logger.info("Kafka Port: ${kafka.brokerPort}")
    logger.info("Kafka Connect REST Port: ${connect.restPort}")
    logger.info("Schema Registry Port: ${connect.schemaRegistry.registryPort}")

//    GlobalScope.launch {
//      fixedRateTimer(daemon = true, period = 5000) {
//        logger.debug(kafka.readRecords("matches", 1))
//      }
//    }

    val keySerde = Serdes.String()
    val avroSerde = GenericAvroSerde().apply {
      configure(mapOf(Pair("schema.registry.url", connect.schemaRegistry.registryUrl)), false)
    }
    val streamsConfig = Properties().apply {
      put(StreamsConfig.APPLICATION_ID_CONFIG, "match-summary")
      put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:${kafka.brokerPort}")
//      put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde)
//      put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde)
    }

    KafkaStreams(StreamsBuilder().apply {
      stream(config["topics"]["match"].textValue(), Consumed.with(keySerde, avroSerde))
//        .mapValues { matchAvro ->
//          logger.info(matchAvro)
//          matchAvro
//        }
        .groupByKey()
        .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
        .aggregate(
          { WindowSummary() },
          { k, v, acc ->
            acc.run {
              val vPrice = v["price"].toString()
              val vTime = v["time"].toString()
              copy(
                highPrice = if (highPrice.isEmpty() || vPrice > highPrice) vPrice else highPrice,
                lowPrice = if (lowPrice.isEmpty() || vPrice < lowPrice) vPrice else lowPrice,
                firstPrice = if (firstPrice.isEmpty() || vTime < firstPriceTime) vPrice else firstPrice,
                firstPriceTime = if (firstPriceTime.isEmpty() || vTime < firstPriceTime) vTime else firstPriceTime,
                lastPrice = if (lastPrice.isEmpty() || vTime > lastPriceTime) vPrice else lastPrice,
                lastPriceTime = if (lastPriceTime.isEmpty() || vTime > lastPriceTime) vTime else lastPriceTime,
                totalValue = totalValue + vPrice.toDouble()
              )
            }
          },
          avroSerde, "minute-summary"
        )
    }.build(), streamsConfig).start()

//    MatchSummary.createTumblingWindow(streamsConfig, config["topics"]["match"].textValue()).start()

    connect.connect.awaitStop()
  }
}
