package coinfeed.types

inline class Milliseconds(val value: Long) {
  fun toSeconds() = Seconds(value / 1000)
  fun toMinutes() = toSeconds().toMinutes()
  fun toHours() = toMinutes().toHours()
  fun toLong() = value
}

inline class Seconds(val value: Long) {
  fun toMilliseconds() = Seconds(value * 1000)
  fun toMinutes() = Minutes(value / 60)
  fun toHours() = toMinutes().toHours()
  fun toLong() = value
}

inline class Minutes(val value: Long) {
  fun toSeconds() = Seconds(value * 60)
  fun toHours() = Hours(value / 60)
  fun toMilliseconds() = toSeconds().toMilliseconds()
  fun toLong() = value
}

inline class Hours(val value: Long) {
  fun toMinutes() = Minutes(value * 60)
  fun toSeconds() = toMinutes().toSeconds()
  fun toMilliseconds() = toSeconds().toMilliseconds()
  fun toLong() = value
}