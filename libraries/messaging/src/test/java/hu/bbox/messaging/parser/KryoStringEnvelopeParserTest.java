package hu.bbox.messaging.parser;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.message.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KryoStringEnvelopeParserTest {
    private final KryoStringEnvelopeSerializer parser = new KryoStringEnvelopeSerializer();

    @Test
    void restore() {
        byte[] bytes = {82, 69, 71, 73, 83, 84, 82, 65, 84, 73, 79, -50, 114, 97, 110, 100, 111, 109, 73, -28};
        Envelope<String> expectedEnvelope = new Envelope<>(MessageType.REGISTRATION, "randomId");
        Envelope<String> restored = parser.deserialize(bytes);
        assertEquals(expectedEnvelope, restored);
    }

    @Test
    void parse() {
        byte[] expectedBytes = {82, 69, 81, 85, 69, 83, -44, 114, 97, 110, 100, 111, 109, 73, -28};
        Envelope<String> envelope = new Envelope<>(MessageType.REQUEST, "randomId");
        byte[] bytes = parser.serialize(envelope);
        assertEquals(expectedBytes.length, bytes.length);
        for (int i = 0; i < expectedBytes.length; i++) {
            byte expectedByte = expectedBytes[i];
            byte actualByte = bytes[i];
            assertEquals(expectedByte, actualByte, "bytes are not matching at idx: " + i);
        }
    }
}