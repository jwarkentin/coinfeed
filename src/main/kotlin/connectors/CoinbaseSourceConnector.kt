package coinfeed.connectors

import coinfeed.tasks.CoinbaseSourceTask
import coinfeed.utils.KafkaAdminOps
import coinfeed.utils.logging.*
import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.connect.source.SourceConnector
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.util.ConnectorUtils
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.Type
import org.apache.kafka.common.config.ConfigDef.Importance
import org.erc.coinbase.pro.rest.ClientConfig

class CoinbaseSourceConnector : SourceConnector() {
  val logger by logger()
  lateinit var connectorProps: Map<String, String>

  companion object {
    fun getVersion() = AppInfoParser.getVersion()
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
      "https://api.pro.coinbase.com",
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
      "",
      Importance.LOW,
      "API passphrase"
    )
  }

  override fun taskConfigs(maxTasks: Int): List<Map<String, String>>
    = (0 until maxTasks).map {
      HashMap(connectorProps)
    }

  override fun start(props: Map<String, String>) {
    connectorProps = HashMap(props)

    val kAdminOps = KafkaAdminOps(connectorProps["bootstrap.servers"]!!)
    if (!kAdminOps.topicExists("products")) {
      kAdminOps.createTopic("products")
    }

    val cbpConfig = ClientConfig().apply {
      setPublicKey(connectorProps["cbproapi.auth.apiKey"])
      setSecretKey(connectorProps["cbproapi.auth.apiSecret"])
      setPassphrase(connectorProps["cbproapi.auth.passphrase"])
    }
  }

  override fun stop() {}
}
