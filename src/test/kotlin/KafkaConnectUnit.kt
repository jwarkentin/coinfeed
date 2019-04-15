package coinfeed

import coinfeed.utils.logging.*
import info.batey.kafka.unit.KafkaUnit
import java.io.File
import java.net.ServerSocket
import java.net.URI
import kotlin.concurrent.thread
import org.apache.kafka.common.utils.SystemTime
import org.apache.kafka.common.utils.Time
import org.apache.kafka.common.utils.Utils
import org.apache.kafka.connect.runtime.Connect
import org.apache.kafka.connect.runtime.Herder
import org.apache.kafka.connect.runtime.Worker
import org.apache.kafka.connect.runtime.rest.RestServer
import org.apache.kafka.connect.runtime.rest.entities.ConnectorInfo
import org.apache.kafka.connect.runtime.isolation.Plugins
import org.apache.kafka.connect.runtime.standalone.StandaloneConfig
import org.apache.kafka.connect.runtime.standalone.StandaloneHerder
import org.apache.kafka.connect.storage.FileOffsetBackingStore
import org.apache.kafka.connect.util.Callback
import org.apache.kafka.connect.util.ConnectUtils
import org.apache.kafka.connect.util.FutureCallback

typealias ConnectorResult = Herder.Created<ConnectorInfo>

class KafkaConnectUnit {
    val logger by logger()
    var kafkaInst: KafkaUnit? = null
    var schemaRegistry: SchemaRegistryUnit
    val workerConfig = HashMap<String, String>()
    val restPort = getEphemeralPort()
    var started = false

    lateinit var connectorInfo: ConnectorInfo
    lateinit var restServer: RestServer
    lateinit var plugins: Plugins
    lateinit var worker: Worker
    lateinit var herder: Herder
    lateinit var connect: Connect

    constructor(kafkaInstance: KafkaUnit? = null, workerCfg: Map<String, String> = mapOf(), registryCfg: Map<String, String> = mapOf()) {
        val registryConfig = mutableMapOf(
            "host.name" to "localhost"
        )

        if (kafkaInstance != null) {
            kafkaInst = kafkaInstance
            workerConfig["bootstrap.servers"] = "localhost:${kafkaInstance.getBrokerPort()}"
            registryConfig["kafkastore.connection.url"] = "localhost:${kafkaInstance.getZkPort()}"
        }

        registryCfg.forEach { registryConfig[it.key] = it.value }
        schemaRegistry = SchemaRegistryUnit(registryConfig)

        val registryUrl = schemaRegistry.registryUrl
        val offsetFile = File.createTempFile("connect-offset-", "-test")
        offsetFile.deleteOnExit()

        workerConfig["schema.registry.url"] = registryUrl
        workerConfig["key.converter"] = "io.confluent.connect.avro.AvroConverter"
        workerConfig["value.converter"] = "io.confluent.connect.avro.AvroConverter"
        workerConfig["key.converter.schema.registry.url"] = registryUrl
        workerConfig["value.converter.schema.registry.url"] = registryUrl
        workerConfig["internal.key.converter"] = "org.apache.kafka.connect.json.JsonConverter"
        workerConfig["internal.value.converter"] = "org.apache.kafka.connect.json.JsonConverter"
        workerConfig["offset.storage.file.filename"] = offsetFile.toString()
        workerConfig["rest.port"] = "${restPort}"

        workerCfg.forEach { workerConfig[it.key] = it.value }
    }

    fun getEphemeralPort() = ServerSocket(0).apply {
        reuseAddress = true
    }.localPort

    fun start() {
        if (started) return
        kafkaInst?.startup()
        logger.debug("Starting Kafka Connect")

        schemaRegistry.start()

        val config: StandaloneConfig = StandaloneConfig(workerConfig)
        restServer = RestServer(config)

        val advertisedUrl = restServer.advertisedUrl()
        val time = SystemTime()
        val workerId = "${advertisedUrl.getHost()}:${advertisedUrl.getPort()}"
        plugins = Plugins(HashMap<String, String>())
        worker = Worker(workerId, time, plugins, config, FileOffsetBackingStore())
        herder = StandaloneHerder(worker, ConnectUtils.lookupKafkaClusterId(config))
        connect = Connect(herder, restServer)

        Runtime.getRuntime().addShutdownHook(thread(false, true) { stop() })
        connect.start()

        started = true
        logger.debug("Kafka Connect started")
    }

    fun stop() {
        if (!started) return
        logger.debug("Stopping Kafka Connect")

        restServer.stop()
        worker.stop()
        herder.stop()
        connect.stop()
        connect.awaitStop()
        schemaRegistry.stop()

        started = false
        logger.debug("Kafka Connect Stopped")
    }

    fun createConnector(connectorConfig: Map<String, String>): Boolean {
        val cName = connectorConfig.get("name")
        if (cName == null) {
            throw Exception("Connector name missing from connector config")
        }
        start()
        logger.debug("Creating connector '${cName}'")

        var success = false
        try {
            herder.putConnectorConfig(
                cName,
                connectorConfig,
                false,
                Callback<ConnectorResult> { error, result ->
                    if (error != null) {
                        logger.error("Failed creating connector '${cName}'", error)
                    } else {
                        connectorInfo = result.result()
                        success = true
                    }
                }
            )
        } catch(e: org.apache.kafka.connect.runtime.rest.errors.BadRequestException) {
            logger.error(e.message)
            throw e
        }

        logger.debug("Created connector '${cName}'")
        return success
    }
}
