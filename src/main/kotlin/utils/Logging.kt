package coinfeed.utils.logging

import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

fun <T> T.logger(): Lazy<Logger> = lazy { LogManager.getLogger(this) }
