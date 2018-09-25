package msg.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.requests.IsolationLevel
import org.apache.kafka.common.serialization.Deserializer
import java.util.Locale
import kotlin.reflect.KClass

open class KafkaConsumer<K,V,DK:Deserializer<K>,DV:Deserializer<V>>(brokers:Collection<Broker>, keyDeserialiser: KClass<DK>, valueDeserialiser: KClass<DV>, clientId: String, groupId: String, vararg config:Pair<String,Any>) : KafkaConsumer<K,V>(mapOf(
  ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers.joinToString(","),
  ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to keyDeserialiser.java.name,
  ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserialiser.java.name,
  ConsumerConfig.CLIENT_ID_CONFIG to clientId,
  ConsumerConfig.GROUP_ID_CONFIG to groupId,
  ConsumerConfig.ISOLATION_LEVEL_CONFIG to IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT),
  *config
))
