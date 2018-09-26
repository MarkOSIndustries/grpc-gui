package msg.kat

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import msg.kafka.TopicIterator
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.MaxOffsetSpec
import msg.kafka.offsets.OffsetSpec
import msg.kafka.offsets.TimestampOffsetSpec
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Instant

class Consume : KafkaTopicDataCommand(help = "Consume records from Kafka\nReads records from Kafka and emits length-prefixed binary records on stdout") {
  private val schema by option("--schema", "-s", help = "the schema name to embed in output records. Only works with --encoding msg.TypedKafkaRecord", metavar = "uses topic name by default")
  private val startOffsetTypes = setOf("earliest","latest")
  private val fromOption by option("--from", "-f", help = "which offsets to start from", metavar="[${startOffsetTypes.joinToString("|")}|<timestampMs>]").default("earliest").validate {
    require(startOffsetTypes.contains(it) || it.toLongOrNull() != null) {"$it isn't a valid offset to start from.\n" +
      "Please choose from $metavar where <timestampMs> means a timestamp like ${Instant.now().toEpochMilli()}"}
  }
  private val endOffsetTypes = setOf("forever","latest")
  private val untilOption by option("--until", "-u", help = "which offsets to end at", metavar="[${endOffsetTypes.joinToString("|")}|<timestampMs>]").default("forever").validate {
    require(endOffsetTypes.contains(it) || it.toLongOrNull() != null) {"$it isn't a valid offset to end at.\n" +
      "Please choose from $metavar where <timestampMs> means a timestamp like ${Instant.now().toEpochMilli()}"}
  }

  override fun run() {
    val write = encoding.writer(System.out)

    TopicIterator(
      newConsumer(ByteArrayDeserializer::class,ByteArrayDeserializer::class),
      topic,
      parseOffsetSpec(fromOption),
      parseOffsetSpec(untilOption)
    ).forEach { write(encoding.fromConsumerRecord(it, schema ?: topic)) }
  }

  private fun parseOffsetSpec(spec:String):OffsetSpec {
    return when(spec) {
      "earliest" -> EarliestOffsetSpec()
      "latest" -> LatestOffsetSpec()
      "forever" -> MaxOffsetSpec()
      else -> TimestampOffsetSpec(spec.toLong())
    }
  }
}
