package coinfeed.utils

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.ListTopicsOptions
import org.apache.kafka.clients.admin.ListTopicsResult
import org.apache.kafka.clients.admin.NewTopic

class KafkaAdminOps(bootstrapServers: String) {
  public val adminClient = AdminClient.create(mapOf(
    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers
  ))

  fun createTopic(name: String, partitions: Int = 1, replicas: Short = 1)
    = adminClient.createTopics(listOf(NewTopic(name, partitions, replicas)))

  fun topicNames(includeInternal: Boolean = false): Set<String> = adminClient.listTopics(
    ListTopicsOptions().apply { listInternal(includeInternal) }
  ).names().get()

  fun topicExists(name: String, searchInternal: Boolean = false)
    = topicNames(searchInternal).contains(name)
}
