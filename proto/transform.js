const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const stream = require('stream')
const streams = require('../streams')
const { matchesFilter } = require('./filter')
const { inspect } = require('util')
module.exports = {
  transform,
}

const streamSchemaObjectsFrom = {
  lengthPrefixedBinary: ({inStream, prefixFormat, converter}) => {
    const outStream = new stream.Writable()
    const prefixedBinaryStream = streams.readLengthPrefixedBuffers(inStream, prefixFormat)
    prefixedBinaryStream.on('data', binaryBuffer => {
      outStream.emit('data', converter.binary_buffer_to_schema_object(binaryBuffer))
    })
    return outStream
  },
  lineDelimitedJson: ({inStream, converter}) => {
    const outStream = new stream.Writable()
    const jsonObjectStream = streams.readLineDelimitedJsonObjects(inStream)
    jsonObjectStream.on('data', jsonObject => {
      outStream.emit('data', converter.json_object_to_schema_object(jsonObject))
    })
    return outStream
  },
  lineDelimitedEncodedBinary: ({inStream, converter, encodingName}) => {
    const outStream = new stream.Writable()
    const linesStream = streams.readUTF8Lines(inStream)
    linesStream.on('data', line => {
      outStream.emit('data', converter.string_encoded_binary_to_schema_object(line, encodingName))
    })
    return outStream
  },
  generator: ({converter}) => {
    const outStream = new stream.Writable()
    setInterval(() => {
      const jsonObject = protobuf.makeValidJsonRecord(converter.schema)
      outStream.emit('data', converter.json_object_to_schema_object(jsonObject))
    }, 1)
    return outStream
  }
}

const streamSchemaObjectsTo = {
  lengthPrefixedBinary: ({inStream, outStream, prefixFormat, filterJsonObject, converter}) => {
    const prefixedBinaryStream = streams.writeLengthPrefixedBuffers(outStream, prefixFormat)

    inStream.on('data', schemaObject => {
      const jsonObject = converter.schema_object_to_json_object(schemaObject)
      if(filterJsonObject(jsonObject)) {
        const binaryBuffer = converter.schema_object_to_binary_buffer(schemaObject)
        prefixedBinaryStream.write(binaryBuffer)
      }
    })
  },
  lineDelimitedJson: ({inStream, outStream, delimiterBuffer, filterJsonObject, stringifyJsonObject, converter}) => {
    const delimitedOutputStream = streams.writeDelimited(outStream, delimiterBuffer)

    inStream.on('data', schemaObject => {
      const jsonObject = converter.schema_object_to_json_object(schemaObject)
      if(filterJsonObject(jsonObject)) {
        delimitedOutputStream.write(stringifyJsonObject(jsonObject))
      }
    })
  },
  lineDelimitedEncodedBinary: ({inStream, outStream, delimiterBuffer, encodingName, filterJsonObject, converter}) => {
    const delimitedOutputStream = streams.writeDelimited(outStream, delimiterBuffer)

    inStream.on('data', schemaObject => {
      const jsonObject = converter.schema_object_to_json_object(schemaObject)
      if(filterJsonObject(jsonObject)) {
        const stringEncodedBinary = converter.schema_object_to_string_encoded_binary(schemaObject, encodingName)
        delimitedOutputStream.write(stringEncodedBinary)
      }
    })
  },
}

const formats = {
  'json': 'lineDelimitedJson',
  'base64': 'lineDelimitedEncodedBinary',
  'hex': 'lineDelimitedEncodedBinary',
  'binary': 'lengthPrefixedBinary',
  'generator': 'generator',
}
const encodings = {
  'base64': 'base64',
  'hex': 'hex',
}

function transform({input, output, schema, prefix, encoding, delimiter, protobufs, filter, template}) {
  const transformConfig = {
    prefixFormat: prefix,
    delimiterBuffer: delimiter,
    filterJsonObject: filter,
    stringifyJsonObject: template,
    converter: new SchemaConverter(protobuf.loadDirectory(protobufs).lookupType(schema)),
    inStream: process.stdin,
    outStream: process.stdout,
  }

  transformConfig.inStream = streamSchemaObjectsFrom[formats[input]](Object.assign({encodingName: encodings[input]}, transformConfig))
  transformConfig.inStream.on('end', () => { process.exit() })

  streamSchemaObjectsTo[formats[output]](Object.assign({encodingName: encodings[output]}, transformConfig))
}
