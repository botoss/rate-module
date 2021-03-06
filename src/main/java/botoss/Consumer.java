package botoss;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        try (Reader propsReader = new FileReader("/kafka.properties")) {
            props.load(propsReader);
        }
        props.put("group.id", "rate-module");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("to-module"));
        logger.info("Subscribed to topic");

        Thread update = new Thread(() -> {
            try {
                RateProducer.rate();
            } catch (IOException e) {
                logger.error("Error on time thread", e);
            }
        });
        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(update, 0, 30, TimeUnit.MINUTES);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                logger.info("record from topic: key = " + record.key() + "; value = " + record.value());
                try {
                    String command = (new JSONObject(record.value())).getString("command").toLowerCase();
                    if (rateCommand(command)) {
                        RateProducer.rate(record);
                    }
                    if (btcCommand(command)) {
                        RateProducer.btc(record);
                    }
                    if (maxBtcCommand(command)) {
                        RateProducer.maxBtc(record);
                    }
                } catch (JSONException e) {
                    logger.error("invalid JSON");
                    continue;
                }
            }
        }
    }

    private static boolean maxBtcCommand(String command) {
        return Arrays.asList("сколькоумаксабитков", "сколькобитковумакса", "maxbtc", "биткимакса").contains(command);
    }

    private static boolean btcCommand(String command) {
        return Arrays.asList("btc", "биток", "битки").contains(command);
    }

    private static boolean rateCommand(String command) {
        return Arrays.asList("kurs", "rate", "курс", "rehc").contains(command);
    }
}
