package msg.kafka.offsets

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

class ConfiguredOffsetSpec(val configuredOffsets: Map<TopicPartition, Long>) : OffsetSpec {
  override fun <K, V> getOffsets(consumer: Consumer<K, V>, topicPartitions: Collection<TopicPartition>): Map<TopicPartition, Long> {
    return configuredOffsets.filter { topicPartitions.contains(it.key) }
  }
}
