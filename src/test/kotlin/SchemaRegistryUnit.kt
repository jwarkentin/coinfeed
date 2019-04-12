package coinfeed

import coinfeed.utils.logging.*
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication
import io.confluent.kafka.schemaregistry.storage.KafkaSchemaRegistry
import java.net.ServerSocket
import org.eclipse.jetty.server.Server

// registryConfig["kafka.connection.url"] is required
class SchemaRegistryUnit(registryCfg: Map<String, String>) {
    val logger by logger()
    var started = false
    val registryConfig = HashMap<String, String>(registryCfg)
    val registryPort = getEphemeralPort()
    var registryUrl = "http://0.0.0.0:${registryPort}"
    lateinit var registryApp: SchemaRegistryRestApplication
    lateinit var registryServer: Server

    init {
        registryConfig["host.name"] = "localhost"
        registryConfig["listeners"] = registryUrl
        registryConfig["kafkastore.topic.replication.factor"] = "1"
    }

    fun getEphemeralPort(): Int {
        val socket = ServerSocket(0)
        socket.setReuseAddress(true)
        return socket.getLocalPort()
    }

    fun start() {
        if (started) return
        logger.debug("Starting Schema Registry")

        val config = SchemaRegistryConfig(registryConfig.toProperties())
        registryApp = SchemaRegistryRestApplication(config)
        registryServer = registryApp.createServer()
        registryServer.start()
        registryServer.setStopAtShutdown(true)

        started = true
        logger.debug("Schema Registry started")
    }

    fun stop() {
        if (!started) return
        logger.debug("Stopping Schema Registry")

        registryServer.stop()

        started = false
        logger.debug("Schema Registry Stopped")
    }

    fun getPort(): Int {
        return registryPort
    }

    // fun getRegistryUrl(): String {
    //     return registryUrl
    // }

    fun registerSchema(): KafkaSchemaRegistry {
        return registryApp.schemaRegistry()
    }
}
