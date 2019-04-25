package coinfeed.connectors.coinbase.models.receive

import com.squareup.moshi.Json
import java.time.OffsetDateTime

interface ChannelMessage {
  val type: MessageType
  val time: String
//  val time: OffsetDateTime
  @Json(name = "product_id")
  val productId: String

  enum class MessageType {
    @Json(name = "error") ERROR,
    @Json(name = "match") MATCH
  }
}

interface SequenceMessage : ChannelMessage {
  val sequence: Long
}