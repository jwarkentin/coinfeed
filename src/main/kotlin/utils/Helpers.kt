package coinfeed.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.kafka.connect.data.Schema
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL
import kotlin.reflect.KProperty

fun <T : Any> cfgToObj(cfg: T): JsonNode? = ObjectMapper(YAMLFactory()).run {
  // NOTE: These implicit `is` casts could be combined with a comma if the type inference was
  //       smart enough but alas, it is not. Still, this is easier than enumerating the various
  //       function overloads.
  when (cfg) {
    is InputStream -> readTree(cfg)
    is Reader -> readTree(cfg)
    is String -> readTree(cfg)
    is File -> readTree(cfg)
    is URL -> readTree(cfg)
    else -> throw Error("Invalid argument type for parameter \"cfg\": ${cfg::class.java}")
  }
}

//fun typeToSchema(value: KProperty<*>): Schema = when {
//  value::class -> if (value)
//  else -> throw Error("Can't map ${value::class} to Kafka schema type")
//}

object JsonHelpers {
  fun <R> nodeToMap(node: JsonNode, valueTransformer: (value: JsonNode) -> R): Map<String, R>
    = node.fields().asSequence().associate {
      it.key to valueTransformer(it.value)
    }

  fun nodeToStringMap(node: JsonNode) = nodeToMap(node) { it.textValue() ?: it.toString() }
}