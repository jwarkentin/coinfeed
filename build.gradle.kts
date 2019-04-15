/**
 * BUILDSCRIPT & PLUGIN CONFIGURATION
 */

plugins {
  kotlin("jvm") version Versions.kotlin
  application
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
  maven {
    url = uri("http://packages.confluent.io/maven/")
  }
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
  testImplementation("org.apache.kafka:kafka_${Versions.scala}")
  testImplementation("org.apache.kafka:connect-runtime")
  testImplementation("info.batey.kafka:kafka-unit") {
    exclude(module = "kafka_2.11")
  }
  testImplementation("io.confluent:kafka-schema-registry")
  testImplementation("io.confluent:kafka-connect-avro-converter")

  // Kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  // App
  // implementation("org.yaml:snakeyaml")

  // Coinbase connector
  implementation("com.github.dubasdey:coinbase-pro-client")
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
    dependency("info.batey.kafka:kafka-unit:${Versions.kafkaUnit}")

    // dependency("io.kotlintest:kotlintest-runner-junit5:${Versions.kotlinTest}")

    dependencySet("org.apache.kafka:${Versions.kafka}") {
      entry("connect-api")
      entry("kafka_${Versions.scala}")
      entry("connect-runtime")
    }

    dependencySet("org.jetbrains.kotlin:${Versions.kotlin}") {
      entry("kotlin-test")
      entry("kotlin-test-junit")
    }

    // dependency("org.yaml:snakeyaml:${Versions.snakeYaml}")

    dependencySet("io.confluent:${Versions.confluent}") {
      entry("kafka-schema-registry")
      entry("kafka-connect-avro-converter")
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
