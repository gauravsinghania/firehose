package io.odpf.firehose.sink.objectstorage.message;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.github.os72.protobuf.dynamic.MessageDefinition;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;
import io.odpf.firehose.sink.objectstorage.proto.TimestampProto;
import io.odpf.firehose.sink.objectstorage.proto.ProtoUtils;

import java.time.Instant;

public class MessageProto {
    private static final String PACKAGE = "io.odpf";
    private static final String FILE_NAME = "booking.proto";
    private static final String TYPE_NAME = "BookingLogMessage";

    public static final String ORDER_NUMBER_FIELD_NAME = "order_number";
    public static final String CREATED_TIME_FIELD_NAME = "created_time";

    public MessageDefinition createMessageDefinition() {
        return MessageDefinition.newBuilder(TYPE_NAME)
                .addField("required", "int64", ORDER_NUMBER_FIELD_NAME, 1)
                .addField("required", TimestampProto.getTypeName(), CREATED_TIME_FIELD_NAME, 2)
                .build();
    }

    public static String getTypeName() {
        return TYPE_NAME;
    }

    private static DynamicSchema createSchema() {
        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder().setName(FILE_NAME).setPackage(PACKAGE);
        MessageDefinition messageDefinition = new MessageProto().createMessageDefinition();
        schemaBuilder.addMessageDefinition(messageDefinition);
        schemaBuilder.addMessageDefinition(TimestampProto.createMessageDefinition());

        DynamicSchema schema;
        try {
            schema = schemaBuilder.build();
        } catch (Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
        return schema;
    }

    public static MessageBuilder createMessageBuilder() {
        DynamicSchema schema = createSchema();
        Descriptors.FileDescriptor fileDescriptor = ProtoUtils.createFileDescriptor(schema);
        Descriptors.Descriptor descriptor = fileDescriptor.findMessageTypeByName(getTypeName());
        return new MessageBuilder(descriptor);
    }

    public static class MessageBuilder {
        private long orderNumber;
        private Instant createdTime;

        private Descriptors.Descriptor descriptor;

        public MessageBuilder(Descriptors.Descriptor descriptor) {
            this.descriptor = descriptor;
        }

        public MessageBuilder setOrderNumber(long orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public MessageBuilder setCreatedTime(Instant createdTime) {
            this.createdTime = createdTime;
            return this;
        }

        public DynamicMessage build() {
            Timestamp timestamp = TimestampProto.newBuilder()
                    .setSeconds(createdTime.getEpochSecond())
                    .setNanos(createdTime.getNano())
                    .build();
            DynamicMessage timestampMessage = DynamicMessage.newBuilder(timestamp).build();

            return DynamicMessage.newBuilder(descriptor)
                    .setField(descriptor.findFieldByName(CREATED_TIME_FIELD_NAME), timestampMessage)
                    .setField(descriptor.findFieldByName(ORDER_NUMBER_FIELD_NAME), orderNumber)
                    .build();
        }
    }
}
