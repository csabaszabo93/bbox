package hu.bbox.messaging.parser;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.message.MessageType;

import java.io.ByteArrayOutputStream;

/**
 * {@link MessageSerializer} implementation to serialise {@link Envelope<String>} instances using Kryo
 */
public class KryoStringEnvelopeSerializer implements MessageSerializer<Envelope<String>> {
    private final Kryo kryo = new Kryo();

    public KryoStringEnvelopeSerializer() {
        kryo.register(Envelope.class, new EnvelopeSerializer());
    }

    @Override
    public Envelope<String> deserialize(byte[] message) {
        Input input = new Input();
        input.setBuffer(message);
        return kryo.readObject(input, Envelope.class);
    }

    @Override
    public byte[] serialize(Envelope<String> message) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Output output = new Output(outputStream)) {
            kryo.writeObject(output, message);
            return output.toBytes();
        }
    }

    private static final class EnvelopeSerializer extends Serializer<Envelope<String>> {

        @Override
        public void write(Kryo kryo, Output output, Envelope<String> envelope) {
            output.writeString(envelope.trackingId());
            output.writeString(envelope.messageType().toString());
            output.writeString(envelope.message());
        }

        @Override
        public Envelope<String> read(Kryo kryo, Input input, Class<? extends Envelope<String>> aClass) {
            String trackingId = input.readString();
            MessageType messageType = MessageType.valueOf(input.readString());
            String message = input.readString();
            return new Envelope<>(trackingId, messageType, message);
        }
    }
}
