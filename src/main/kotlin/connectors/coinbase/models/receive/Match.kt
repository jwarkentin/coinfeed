package coinfeed.connectors.coinbase.models.receive

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime

@JsonClass(generateAdapter = true)
data class Match(
  override val type: ChannelMessage.MessageType,
  override val time: String,
//  override val time: OffsetDateTime,
  @Json(name = "product_id")
  override val productId: String,
  override val sequence: Long,
  @Json(name = "trade_id")
  val tradeId: Long,
  @Json(name = "maker_order_id")
  val makerOrderId: String,
  @Json(name = "taker_order_id")
  val takerOrderId: String,
  val size: String,
  val price: String,
  val side: Side
) : SequenceMessage {
  enum class Side {
    @Json(name = "buy") BUY,
    @Json(name = "sell") SELL
  }
}