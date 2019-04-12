package coinfeed.tasks

import coinfeed.connectors.CoinbaseSourceConnector
import coinfeed.utils.logging.*
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask

class CoinbaseSourceTask : SourceTask() {
  val logger by logger()
  lateinit private var taskConfig: Map<String, String>

  companion object {
    fun connectorVersion() = CoinbaseSourceConnector.getVersion()
  }

  override fun version() = connectorVersion()

  override fun start(props: Map<String, String>) {
      taskConfig = props
  }

  override fun stop() {}

  override fun poll(): List<SourceRecord> {
      val records = mutableListOf<SourceRecord>()
      return records
  }
}
