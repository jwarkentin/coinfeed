package coinfeed.models

data class WindowSummary(
  // highest price, lowest price, first price, last price, and total amount, as well as a timestamp for the beginning of the window
//  val windowStart: Long,
//  val windowDuration: Long,
  val highPrice: String = "",
  val lowPrice: String = "",
  val firstPrice: String = "",
  val firstPriceTime: String = "",
  val lastPrice: String = "",
  val lastPriceTime: String = "",
  val totalValue: Double = 0.0
)