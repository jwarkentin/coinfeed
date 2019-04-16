package coinfeed.connectors

import coinfeed.tasks.CoinbaseSourceTask
import coinfeed.types.Hours
import coinfeed.types.Minutes
import coinfeed.utils.KafkaAdminOps
import coinfeed.utils.logging.*
import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.connect.source.SourceConnector
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.Type
import org.apache.kafka.common.config.ConfigDef.Importance
import org.erc.coinbase.pro.model.Product
import org.erc.coinbase.pro.rest.ClientConfig
import org.erc.coinbase.pro.rest.RESTClient
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.fixedRateTimer

class CoinbaseSourceConnector : SourceConnector() {
  private val logger by logger()
  private val connectorProps = HashMap(config().defaultValues()) as HashMap<String, String?>
  private lateinit var products: List<Product>
  private lateinit var productsTimer: Timer

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
  }

  override fun taskConfigs(maxTasks: Int): List<Map<String, String?>>
    = (0 until maxTasks).map { connectorProps }

  override fun start(props: Map<String, String>) {
    connectorProps.putAll(props)
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
        if (products.size != oldProducts.size) {
          context.requestTaskReconfiguration()
        }
      }
    }

//    val kAdminOps = KafkaAdminOps(connectorProps["bootstrap.servers"]!!)
//    if (!kAdminOps.topicExists("products")) {
//      kAdminOps.createTopic("products")
//    }
  }

  override fun stop() {
    productsTimer.cancel()
  }

  //
  // Additional functionality
  //

  private fun getRestClient() = RESTClient(ClientConfig().apply {
    baseUrl = connectorProps["cbproapi.url"]
    publicKey = connectorProps["cbproapi.auth.apiKey"]
    secretKey = connectorProps["cbproapi.auth.apiSecret"]
    passphrase = connectorProps["cbproapi.auth.passphrase"]
  })

  private fun getProducts() = getRestClient().getProducts(null)
}
