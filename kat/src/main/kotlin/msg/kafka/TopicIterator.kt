package msg.kafka

import msg.kafka.offsets.OffsetSpec
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.util.LinkedList

class TopicIterator<K,V>(private val consumer: Consumer<K, V>, private val topic:String, startOffsetSpec: OffsetSpec, endOffsetSpec: OffsetSpec) : Iterator<ConsumerRecord<K, V>> {
  private val partitions = consumer.topicPartitions(topic, Duration.ofMinutes(1)).toMutableSet()
  private val startOffsets = startOffsetSpec.getOffsets(consumer, partitions)
  private val endOffsets = endOffsetSpec.getOffsets(consumer, partitions)
  private val records = LinkedList<ConsumerRecord<K,V>>()

  init {
    if(partitions.isEmpty()) {
      throw NoSuchTopicException(topic)
    }
    consumer.assign(partitions)
    startOffsets.forEach(consumer::seek)

    ensureQueueDoesntRunEmpty()
  }

  override fun hasNext() = records.isNotEmpty()

  override fun next(): ConsumerRecord<K, V> {
    ensureQueueDoesntRunEmpty()
    return records.pop()
  }

  private fun ensureQueueDoesntRunEmpty() {
    while(partitions.isNotEmpty() && records.size < 2) {
      if(partitions.size != consumer.assignment().size) {
        consumer.assign(partitions)
      }
      val batch = consumer.poll(Duration.ofSeconds(10))
      batch.records(topic).forEach { record ->
        if(record.offset() < endOffsets[record.topicPartition()]!!) {
          records.push(record)
        } else {
          partitions.remove(record.topicPartition())
        }
      }
    }
  }
}

private fun <K, V> ConsumerRecord<K, V>.topicPartition(): TopicPartition  = TopicPartition(topic(), partition())
