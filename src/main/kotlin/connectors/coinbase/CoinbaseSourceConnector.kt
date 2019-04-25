package coinfeed.connectors.coinbase

import coinfeed.types.Hours
import coinfeed.types.Minutes
import coinfeed.utils.JsonHelpers
import coinfeed.utils.KafkaAdminOps
import coinfeed.utils.cfgToObj
import coinfeed.utils.logging.*
import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.connect.source.SourceConnector
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.Type
import org.apache.kafka.common.config.ConfigDef.Importance
import org.apache.kafka.connect.util.ConnectorUtils
import org.erc.coinbase.pro.model.Product
import org.erc.coinbase.pro.rest.ClientConfig
import org.erc.coinbase.pro.rest.RESTClient
import java.util.*
import kotlin.concurrent.fixedRateTimer

class CoinbaseSourceConnector : SourceConnector() {
  private val logger by logger()
  private lateinit var connectorConfig: Map<String, String>
  private lateinit var products: List<Product>
  private lateinit var productsTimer: Timer
  private lateinit var topicMap: Map<String, String>

  companion object {
    fun getVersion() = AppInfoParser.getVersion()!!
  }

  override fun version() = getVersion()
  override fun taskClass(): Class<out Task> = CoinbaseSourceTask::class. java

  override fun config() = ConfigDef().apply {
    define(
      "bootstrap.servers",
      Type.STRING,
      Importance.HIGH,
      "Bootstrap servers"
    )

    define(
      "cbproapi.url",
      Type.STRING,
      "https://api-public.sandbox.pro.coinbase.com",
      Importance.HIGH,
      "API URL"
    )

    define(
      "cbproapi.websocket.url",
      Type.STRING,
      "wss://ws-feed-public.sandbox.pro.coinbase.com",
      Importance.HIGH,
      "Websocket feed URL"
    )

    define(
      "cbproapi.auth.apiKey",
      Type.STRING,
      Importance.HIGH,
      "API public key"
    )

    define(
      "cbproapi.auth.apiSecret",
      Type.STRING,
      Importance.HIGH,
      "API secret key"
    )

    define(
      "cbproapi.auth.passphrase",
      Type.STRING,
      Importance.HIGH,
      "API passphrase"
    )

    define(
      "createMissingTopics",
      Type.BOOLEAN,
      false,
      Importance.HIGH,
      "Auto-create missing topics"
    )

    define(
      "topics",
      Type.STRING,
      Importance.HIGH,
      "Map of events to save and what topics to save them to"
    )
  }

  override fun taskConfigs(maxTasks: Int): List<Map<String, String?>>
    = ConnectorUtils.groupPartitions(products.associateBy { it.id }.keys.toList(), maxTasks).map { taskGroup ->
      mapOf(
        "productIds" to taskGroup.joinToString(","),
        "websocketUrl" to connectorConfig["cbproapi.websocket.url"],
        "topics" to connectorConfig["topics"]
      )
    }

  override fun start(props: Map<String, String>) {
    connectorConfig = config().parse(props)
      .also {
        topicMap = JsonHelpers.nodeToStringMap(cfgToObj(it["topics"]!!)!!)
      }
      .let { cfg ->
        cfg.mapValues { it.value.toString() }
      }

    products = getProducts()
    productsTimer = fixedRateTimer(
      "products-refresh",
      true,
      Minutes(1).toMilliseconds().toLong(),
      Hours(1).toMilliseconds().toLong()
    ) {
      getProducts().let {
        val oldProducts = products
        products = it
        // NOTE/TODO: This is quick and dirty for the moment but we should really compare the actual product names.
        //            The size of the list could hypothetically match while still not being a matching list.
        if (products.size != oldProducts.size) {
          context.requestTaskReconfiguration()
        }
      }
    }

    val kAdminOps = KafkaAdminOps(connectorConfig["bootstrap.servers"]!!)
    topicMap.forEach {
      val topicName = it.value
      if (!kAdminOps.topicExists(topicName)) {
        kAdminOps.createTopic(topicName)
        logger.info("Created topic '$topicName'")
      }
    }
  }

  override fun stop() {
    productsTimer.cancel()
  }

  //
  // Additional functionality
  //

  private fun getRestClient() = RESTClient(ClientConfig().apply {
    baseUrl = connectorConfig["cbproapi.url"]
    publicKey = connectorConfig["cbproapi.auth.apiKey"]
    secretKey = connectorConfig["cbproapi.auth.apiSecret"]
    passphrase = connectorConfig["cbproapi.auth.passphrase"]
  })

  private fun getProducts() = getRestClient().getProducts(null)
}
