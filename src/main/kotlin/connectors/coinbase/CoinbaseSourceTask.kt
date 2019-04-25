package coinfeed.connectors.coinbase

import coinfeed.connectors.coinbase.models.receive.Match
import coinfeed.connectors.coinbase.models.send.Subscribe
import coinfeed.utils.JsonHelpers
import coinfeed.utils.cfgToObj
import coinfeed.utils.logging.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Message
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class CoinbaseSourceTask : SourceTask() {
  private enum class TaskState {
    STOPPED, STARTED
  }

  val logger by logger()
  private val moshi: Moshi = Moshi.Builder().build()
  private val matchAdapter = moshi.adapter(Match::class.java).apply { lenient() }
  private val backoffStrategy = ExponentialWithJitterBackoffStrategy(1000, 10000)
  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .build()
  private var recordBuffer: List<SourceRecord> = listOf()

  // These are variables that can change between calls to `stop()` and `start()`
  private var state: TaskState = TaskState.STOPPED
  private lateinit var taskConfig: Map<String, String>
  private lateinit var topicMap: Map<String, String>
  private lateinit var feedService: CoinbaseProService

  companion object {
    fun connectorVersion() = CoinbaseSourceConnector.getVersion()
  }

  override fun version() = connectorVersion()

  override fun start(props: Map<String, String>) {
    taskConfig = props.also {
      topicMap = JsonHelpers.nodeToStringMap(cfgToObj(it["topics"]!!)!!)
    }

    feedService = Scarlet.Builder()
      .webSocketFactory(okHttpClient.newWebSocketFactory(taskConfig["websocketUrl"]!!))
      .addMessageAdapterFactory(MoshiMessageAdapter.Factory(moshi))
      .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
      .backoffStrategy(backoffStrategy)
      .build()
      .create()

    GlobalScope.launch {
      for (event in feedService.webSocketEvents()) {
        // For now we will just stop reading from the stream which will force the server to close the connection
        if (state == TaskState.STOPPED) break

        when (event) {
          is WebSocket.Event.OnConnectionOpened<*> -> {
            feedService.subscribe(Subscribe(
              productIds = props["productIds"].toString().split(","),
              channels = listOf("full")
            ))
          }
          is WebSocket.Event.OnConnectionClosed,
          is WebSocket.Event.OnConnectionFailed ->
            throw Error("Connection closed or failed: $event")
          is WebSocket.Event.OnMessageReceived -> withContext(coroutineContext) {
            handleMessageEvent(event.message as Message.Text)
          }
        }
      }
    }

    state = TaskState.STARTED

//    feedService.observeWebSocketEvent().consumeEach {  }
//      .filter { it is WebSocket.Event.OnConnectionOpened<*> }
//      .subscribe({
//        gdaxService.sendSubscribe(BITCOIN_TICKER_SUBSCRIBE_MESSAGE)
//      })
  }

  // TODO: Implement proper stop. Needs to disconnect from websocket which is harder than it should be with Scarlet.
  //       See: https://github.com/Tinder/Scarlet/issues/48
  override fun stop() {
    state = TaskState.STOPPED
  }

  override fun poll(): List<SourceRecord>? {
    lateinit var records: List<SourceRecord>
    runBlocking {
      withContext(coroutineContext) {
        records = recordBuffer
        recordBuffer = listOf()
      }
    }
    return (if (records.isNotEmpty()) records else null)?.also {
      logger.debug("Poll() sending records: $records")
    }
  }

  private fun handleMessageEvent(event: Message.Text) {
    try {
      when (val eventRecord = matchAdapter.fromJson(event.value)) {
        is Match -> with(eventRecord) {
          logger.debug("Adding record to batch: $this")
          recordBuffer = recordBuffer.plus(SourceRecord(
            mapOf("productId" to productId),
            mapOf("sequence" to sequence),
            topicMap[type.name.toLowerCase()]!!,
            null,
            Schemas.MATCH_KEY,
            Struct(Schemas.MATCH_KEY).put("productId", productId),
            Schemas.MATCH_VALUE,
            Struct(Schemas.MATCH_VALUE).apply {
              val getters = eventRecord.javaClass.methods.associateBy { it.name }
              schema().fields().forEach {
                val fieldName = it.name()
                val fieldType = it.schema()
                getters["get${fieldName.capitalize()}"]?.apply {
//                  logger.trace("$fieldName [$fieldType]: ${invoke(eventRecord)}")
                  val fieldValue = invoke(eventRecord)
                  put(fieldName, if (fieldType == Schema.STRING_SCHEMA) fieldValue.toString() else fieldValue)
                }
              }
            },
            OffsetDateTime.parse(time).toInstant().toEpochMilli()
          ))
        }
      }
    } catch (e: JsonDataException) {
      // Ignore message types we don't understand but log it
      logger.debug("Received unknown message type: ${event.value}")
    }
  }
}
