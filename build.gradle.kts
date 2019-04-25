/**
 * BUILDSCRIPT & PLUGIN CONFIGURATION
 */

plugins {
  kotlin("jvm") version Versions.kotlin
  kotlin("kapt") version Versions.kotlin
  application
  idea
  id("io.spring.dependency-management") version Versions.dependencyManagement
}

buildscript {
  repositories {
    mavenLocal()
    gradlePluginPortal()    // See https://github.com/gradle/gradle/issues/4020#issuecomment-357489131
    jcenter()
  }

  dependencies {}
}

repositories {
  mavenLocal()
  gradlePluginPortal()    // See https://github.com/gradle/gradle/issues/4020#issuecomment-357489131
  jcenter()
  maven { url = uri("http://packages.confluent.io/maven/") }
  maven { url = uri("https://jitpack.io") }
}

application {
  mainClassName = "coinfeed.AppKt"
  applicationDefaultJvmArgs = listOf(
    "-Dlog4j.configurationFile=log4j2.yml",
    "-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
  )
}

/**
 * BUILD CONFIGURATION
 */

dependencies {
  // Logging
  implementation("org.apache.logging.log4j:log4j-api")
  implementation("org.apache.logging.log4j:log4j-core")
  implementation("org.apache.logging.log4j:log4j-jul")
  implementation("org.apache.logging.log4j:log4j-slf4j-impl")

  // Testing
  // testImplementation("io.kotlintest:kotlintest-runner-junit5")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

  // Needed for log4j2 YAML configuration files
  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

  // Kafka
  implementation("org.apache.kafka:connect-api")
  implementation("org.apache.kafka:connect-runtime")
  implementation("org.apache.kafka:kafka-streams")
  implementation("io.confluent:kafka-connect-avro-converter")
  implementation("io.confluent:kafka-streams-avro-serde")
  testImplementation("org.apache.kafka:kafka_${Versions.scala}")
  testImplementation("info.batey.kafka:kafka-unit") {
    exclude(module = "kafka_2.11")
  }
  testImplementation("io.confluent:kafka-schema-registry")

  // Kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  // Coinbase connector
  implementation("com.github.dubasdey:coinbase-pro-client")
  implementation("com.github.tinder.scarlet:scarlet")
  implementation("com.github.tinder.scarlet:scarlet-stream-adapter-coroutines")
  implementation("com.github.tinder.scarlet:scarlet-message-adapter-moshi")
  implementation("com.github.tinder.scarlet:scarlet-websocket-okhttp")

  implementation("com.squareup.moshi:moshi-kotlin")
  kapt("com.squareup.moshi:moshi-kotlin-codegen")
}

dependencyManagement {
  dependencies {
    dependencySet("org.apache.logging.log4j:${Versions.log4j}") {
      entry("log4j-api")
      entry("log4j-core")
      entry("log4j-jul")
      entry("log4j-slf4j-impl")
    }

    dependencySet("com.fasterxml.jackson.core:${Versions.jackson}") {
      entry("jackson-core")
      entry("jackson-databind")
    }
    dependency("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")
    dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.jackson}")

    dependency("com.github.dubasdey:coinbase-pro-client:${Versions.cbpClient}")

    dependencySet("com.github.tinder.scarlet:${Versions.scarlet}") {
      entry("scarlet")
      entry("scarlet-message-adapter-moshi")
      entry("scarlet-stream-adapter-coroutines")
      entry("scarlet-websocket-okhttp")
    }

    dependencySet("com.squareup.moshi:${Versions.moshi}") {
      entry("moshi-kotlin")
      entry("moshi-kotlin-codegen")
    }

    dependency("info.batey.kafka:kafka-unit:${Versions.kafkaUnit}")

    // dependency("io.kotlintest:kotlintest-runner-junit5:${Versions.kotlinTest}")

    dependencySet("org.apache.kafka:${Versions.kafka}") {
      entry("connect-api")
      entry("connect-runtime")
      entry("kafka_${Versions.scala}")
      entry("kafka-streams")
    }

    dependencySet("org.jetbrains.kotlin:${Versions.kotlin}") {
      entry("kotlin-test")
      entry("kotlin-test-junit")
    }

    // dependency("org.yaml:snakeyaml:${Versions.snakeYaml}")

    dependencySet("io.confluent:${Versions.confluent}") {
      entry("kafka-schema-registry")
      entry("kafka-connect-avro-converter")
      entry("kafka-streams-avro-serde")
    }
  }
}

/**
 * TASKS
 */

tasks.test {
  // useJUnitPlatform()

  testLogging {
    showStandardStreams = true
  }

  // useJUnit()
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}
