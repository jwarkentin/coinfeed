package coinfeed.connectors.coinbase

import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder

object Schemas {
  val MATCH_KEY_NAME = "coinfeed.MatchKey"
  val MATCH_VALUE_NAME = "coinfeed.MatchValue"

  val MATCH_KEY = SchemaBuilder.struct().name(MATCH_KEY_NAME).version(1)
    .field("productId", Schema.STRING_SCHEMA)
    .build()
  val MATCH_VALUE = SchemaBuilder.struct().name(MATCH_VALUE_NAME).version(1)
    .apply {
      field("type", Schema.STRING_SCHEMA)
      field("time", Schema.STRING_SCHEMA)
      field("productId", Schema.STRING_SCHEMA)
      field("sequence", Schema.INT64_SCHEMA)
      field("tradeId", Schema.INT64_SCHEMA)
      field("makerOrderId", Schema.STRING_SCHEMA)
      field("takerOrderId", Schema.STRING_SCHEMA)
      field("size", Schema.STRING_SCHEMA)
      field("price", Schema.STRING_SCHEMA)
      field("side", Schema.STRING_SCHEMA)
    }
    .build()
}