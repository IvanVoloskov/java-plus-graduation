package ewm.stats.analyzer.kafka;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class AvroDeserializer implements Deserializer<SpecificRecordBase> {

    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";
    private static final String EVENTS_SIMILARITY_TOPIC = "stats.events-similarity.v1";

    private final DecoderFactory decoderFactory = DecoderFactory.get();

    @Override
    public SpecificRecordBase deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        DatumReader<? extends SpecificRecordBase> reader = switch (topic) {
            case USER_ACTIONS_TOPIC -> new SpecificDatumReader<>(UserActionAvro.getClassSchema());
            case EVENTS_SIMILARITY_TOPIC -> new SpecificDatumReader<>(EventSimilarityAvro.getClassSchema());
            default -> throw new SerializationException("Неизвестный топик: " + topic);
        };

        try {
            BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new SerializationException("Ошибка десериализации из топика " + topic, e);
        }
    }
}