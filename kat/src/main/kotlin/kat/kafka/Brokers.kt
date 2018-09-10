package kat.kafka

object Brokers {
  fun from(csvBrokers: String):Array<Broker> =
    csvBrokers.split(",").map { broker ->
      val hostAndPort = broker.split(":").map { it.trim() }
      if(hostAndPort.size == 1) {
        Broker(hostAndPort[0], 9092)
      } else {
        Broker(hostAndPort[0], hostAndPort[1].toInt())
      }
    }.toTypedArray()
}
