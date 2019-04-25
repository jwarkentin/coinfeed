package coinfeed.views

import coinfeed.connectors.coinbase.Schemas.MATCH_KEY
import coinfeed.connectors.coinbase.Schemas.MATCH_VALUE
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.kstream.Windowed
import java.time.Duration
import java.util.*

class MatchSummary(val config: Properties, val topic: String) {
  fun createTumblingWindow() = KafkaStreams(
    StreamsBuilder().apply {
      stream<String, String>(topic)
        .groupByKey()
        .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
        .reduce { v1, v2 -> if (v2 > v1) v2 else v1 }

//      table<String, String>(topic)
//        .groupBy()
    }.build(),
    config
  )
}