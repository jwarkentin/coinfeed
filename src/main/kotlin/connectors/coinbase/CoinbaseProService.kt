package coinfeed.connectors.coinbase

import coinfeed.connectors.coinbase.models.receive.Match
import coinfeed.connectors.coinbase.models.send.Subscribe
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.channels.ReceiveChannel

interface CoinbaseProService {
  @Receive
  fun webSocketEvents(): ReceiveChannel<WebSocket.Event>

  @Send
  fun subscribe(options: Subscribe)

//  @Receive
//  fun observeMatches(): ReceiveChannel<Match>
}