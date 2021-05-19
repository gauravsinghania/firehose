package io.odpf.firehose.sink.file;

import com.gojek.de.stencil.parser.Parser;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.odpf.firehose.consumer.Message;
import io.odpf.firehose.exception.DeserializerException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MessageToRecord implements Serializer {

    private MetadataFactory metadataFactory;
    private final boolean doWriteKafkaMetadata;
    private Parser protoParser;

    public Record serialize(Message message) throws DeserializerException {
        try {
            DynamicMessage dynamicMessage = protoParser.parse(message.getLogMessage());

            DynamicMessage kafkaMetadata = null;
            if (doWriteKafkaMetadata) {
                kafkaMetadata = metadataFactory.create(message);
            }
            return new Record(dynamicMessage, kafkaMetadata);
        } catch (InvalidProtocolBufferException e) {
            throw new DeserializerException("", e);
        }
    }
}
