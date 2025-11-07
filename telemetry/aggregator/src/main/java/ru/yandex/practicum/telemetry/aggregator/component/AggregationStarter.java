package ru.yandex.practicum.telemetry.aggregator.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.VoidDeserializer;
import org.apache.kafka.common.serialization.VoidSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.serializer.GenericAvroSerializer;
import ru.yandex.practicum.kafka.serializer.SensorEventDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.*;


@Slf4j
@Component
public class AggregationStarter {

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();
    private final String topicIn;
    private final String topicOut;
    private final String server;
    private Producer<Void, SpecificRecordBase> producer;
    private KafkaConsumer<Void, SpecificRecordBase> consumer;

    @Autowired
    public AggregationStarter(@Value("${collector.kafka.topic.sensors}") String topicIn,
                              @Value("${collector.kafka.topic.snapshots}") String topicOut,
                              @Value("${collector.kafka.server}") String server) {
        this.topicIn = topicIn;
        this.topicOut = topicOut;
        this.server = server;
    }

    public void start() {
        initConsumer();
        initProducer();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(topicIn));
            while (true) {
                ConsumerRecords<Void, SpecificRecordBase> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                for (ConsumerRecord<Void, SpecificRecordBase> record : records) {
                    // обрабатываем очередную запись
                    updateState(record).ifPresent(value -> producer.send(new ProducerRecord<>(topicOut, value)));
                }
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
            // Do nothing
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
                producer.close(Duration.ofSeconds(10));
                consumer.commitSync();
                consumer.close(Duration.ofSeconds(10));
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private void initProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, VoidSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GenericAvroSerializer.class);

        producer = new KafkaProducer<>(config);
    }

    private void initConsumer() {
        Properties config = new Properties();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "aggregatorConsumer");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator.group");
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, VoidDeserializer.class.getCanonicalName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorEventDeserializer.class.getCanonicalName());

        consumer = new KafkaConsumer<>(config);
    }

    private Optional<SensorsSnapshotAvro> updateState(ConsumerRecord<Void, SpecificRecordBase> record) {
        SensorEventAvro event = (SensorEventAvro) record.value();
        SensorsSnapshotAvro snapshot = snapshots.get(event.getHubId());
        if (snapshot == null) {
            snapshot = SensorsSnapshotAvro.newBuilder()
                    .setHubId(event.getHubId())
                    .setTimestamp(Instant.now())
                    .setSensorsState(new HashMap<>())
                    .build();
            snapshots.put(event.getHubId(), snapshot);
        }

        SensorStateAvro oldState = snapshot.getSensorsState().get(event.getId());
        if (oldState != null
                && (oldState.getTimestamp().isAfter(event.getTimestamp()) || oldState.getData().equals(event.getPayload()))) {
            return Optional.empty();
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setData(event.getPayload())
                .setTimestamp(event.getTimestamp())
                .build();
        snapshot.getSensorsState().put(event.getId(), newState);
        snapshot.setTimestamp(event.getTimestamp());
        return Optional.of(snapshot);
    }
}
