package ewm.stats.aggregator.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
public class AvroDeserializer implements Deserializer<UserActionAvro> {
    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final DatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.getClassSchema());

    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
            UserActionAvro result = reader.read(null, decoder);
            return result;
        } catch (Exception e) {
            throw new SerializationException("Ошибка десериализации Avro из топика " + topic, e);
        }
    }
}
