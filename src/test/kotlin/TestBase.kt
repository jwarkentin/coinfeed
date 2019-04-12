package coinfeed

import coinfeed.utils.logging.*

abstract class TestBase {
  val logger by logger()
  val kafkaInsts = HashMap<String, KafkaUnit>()
  val connectInsts = HashMap<String, KafkaConnectUnit>()

  fun getKafka(name: String = "default", zkPort: Int = 0, brokerPort: Int = 0)
    = kafkaInsts[name] ?: kafkaInsts.let {
      (
        if (zkPort > 0 && brokerPort > 0)
          KafkaUnit(zkPort, brokerPort)
        else
          KafkaUnit()
      ).also {
        kafkaInsts[name] = it
      }
    }

  fun getKafkaConnect(name: String = "default", kafkaInst: KafkaUnit = getKafka(name), workerConfig: Map<String, String>? = null): KafkaConnectUnit {
    var inst: KafkaConnectUnit? = connectInsts[name]

    if (inst == null) {
      inst = KafkaConnectUnit(kafkaInst, workerConfig ?: mapOf())
      connectInsts[name] = inst
    }

    return inst
  }
}
