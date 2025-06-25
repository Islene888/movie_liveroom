package com.ella.flinkjob;

import com.ella.flinkjob.config.KafkaSinkUtil;
import com.ella.flinkjob.service.LiveEventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FlinkEventTypeAggregator {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:29092")
                .setTopics("live-events")
                .setGroupId("flink-eventtype-forwarder-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 1. 源数据 print
        DataStream<String> rawStream = env.fromSource(
                        kafkaSource,
                        WatermarkStrategy.noWatermarks(),
                        "Kafka Source"
                )
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty());

        rawStream.print("rawStream");

        // 2. map 结果 print
        DataStream<Tuple2<String, Integer>> aggStream = rawStream
                .map(LiveEventProcessor.eventTypeMapper())
                .filter(x -> x != null);

        aggStream.print("aggStream");

        String bootstrapServers = "kafka:29092";
        String outputTopic = "live-events-agg";

        // 3. 窗口聚合产物 print
        DataStream<String> windowed = aggStream
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(1)))
                .process(new ProcessAllWindowFunction<Tuple2<String, Integer>, String, TimeWindow>() {
                    @Override
                    public void process(Context context, Iterable<Tuple2<String, Integer>> elements, Collector<String> out) throws Exception {
                        Map<String, Integer> stats = new HashMap<>();
                        stats.put("like", 0);
                        stats.put("comment", 0);
                        stats.put("user_join", 0);
                        stats.put("send_gift", 0);

                        for (Tuple2<String, Integer> element : elements) {
                            if (element != null && element.f0 != null) {
                                stats.compute(element.f0, (key, oldValue) -> (oldValue == null) ? element.f1 : oldValue + element.f1);
                            }
                        }
                        ObjectMapper om = new ObjectMapper();
                        String result = om.writeValueAsString(stats);
                        out.collect(result);
                    }
                });

        windowed.print("windowed"); // <---- 再 print 一遍，极限排查

        windowed.sinkTo(KafkaSinkUtil.createKafkaSink(bootstrapServers, outputTopic));
        env.execute("Flink Kafka EventType Forwarder");
    }
}
