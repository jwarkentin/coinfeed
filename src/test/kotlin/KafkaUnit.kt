package coinfeed

import coinfeed.utils.logging.*
import info.batey.kafka.unit.KafkaUnit as KafkaBase

class KafkaUnit : KafkaBase {
    val logger by logger()
    private var started = false

    constructor() : super()
    constructor(zkPort: Int, brokerPort: Int, zkMaxConnections: Int = 16) : super(zkPort, brokerPort, zkMaxConnections)
    constructor(zkConnectionString: String, kafkaConnectionString: String) : super(zkConnectionString, kafkaConnectionString)

    override fun startup() {
        if (started) return
        logger.debug("Starting Kafka")
        started = true
        super.startup()
        logger.debug("Kafka Started")
    }

    override fun shutdown() {
        if (!started) return
        logger.debug("Stopping Kafka")
        super.shutdown()
        started = false
        logger.debug("Kafka Stopped")
    }
}
