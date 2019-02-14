package msg.qs.encodings

import msg.schemas.MSG
import java.io.InputStream
import java.io.PrintStream

class PutRequest : Encoding {
  override fun decodeKeyValuePair(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    val record = MSG.PutRequest.parseFrom(bytes)
    return record.key.toByteArray() to record.value.toByteArray()
  }

  override fun reader(input: InputStream): Iterator<ByteArray> {
    return LengthPrefixedByteArrayIterator(input)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return Encoding.lengthPrefixedBinaryValues(output)
  }
}