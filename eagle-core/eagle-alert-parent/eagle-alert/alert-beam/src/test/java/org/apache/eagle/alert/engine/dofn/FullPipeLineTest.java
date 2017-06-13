package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestStream;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.*;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class FullPipeLineTest {
    @Rule
    public final transient TestPipeline p = TestPipeline.create();
    private int numOfRouterBolts = 10;
    private final TupleTag<SpoutSpec> spoutSpecTupleTag = new TupleTag<SpoutSpec>() {

    };
    private final TupleTag<RouterSpec> routerSpecTupleTag = new TupleTag<RouterSpec>() {

    };
    private final TupleTag<AlertBoltSpec> alertBoltSpecTupleTag = new TupleTag<AlertBoltSpec>() {

    };
    private final TupleTag<PublishSpec> publishSpecTupleTag = new TupleTag<PublishSpec>() {

    };
    private final TupleTag<Map<String, StreamDefinition>> sdsTag = new TupleTag<Map<String, StreamDefinition>>() {

    };
    private final TupleTag<List<StreamPartition>> spTag = new TupleTag<List<StreamPartition>>() {

    };

    private final TupleTag<Map<StreamPartition, StreamSortSpec>> sssTag = new TupleTag<Map<StreamPartition, StreamSortSpec>>() {

    };
    private final TupleTag<Map<StreamPartition, List<StreamRouterSpec>>> srsTag = new TupleTag<Map<StreamPartition, List<StreamRouterSpec>>>() {

    };

    @Test
    public void testFullPipeLine() {
        long starttime = 1496638588877L;
        TestStream<KV<String, String>> source = TestStream
                .create(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())).addElements(KV.of("oozie",
                        "{\"ip\":\"yyy.yyy.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":\""
                                + starttime + "\"}")).advanceWatermarkToInfinity();

        PCollection<KV<String, String>> rawMessage = p.apply("get config by source", source);


        PCollectionTuple rs = rawMessage.apply("get config windows",
                Window.<KV<String, String>>into(new GlobalWindows()).triggering(
                        AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(10)))
                        .discardingFiredPanes().withAllowedLateness(Duration.ZERO)).apply(
                ParDo.of(new GetConfigFromFileFn(spoutSpecTupleTag, sdsTag, spTag, routerSpecTupleTag, publishSpecTupleTag, sssTag, srsTag,alertBoltSpecTupleTag))
                        .withOutputTags(spoutSpecTupleTag, TupleTagList.of(sdsTag).and(spTag).and(routerSpecTupleTag).and(publishSpecTupleTag).and(sssTag).and(srsTag).and(alertBoltSpecTupleTag)));


        PCollection<SpoutSpec> spoutSpec = rs.get(spoutSpecTupleTag);
        PCollection<RouterSpec> routerSpec = rs.get(routerSpecTupleTag);
        PCollection<PublishSpec> publishSpec = rs.get(publishSpecTupleTag);
        PCollection<AlertBoltSpec> alertBoltSpec = rs.get(alertBoltSpecTupleTag);
        PCollection<Map<String, StreamDefinition>> sds = rs.get(sdsTag);
        PCollection<List<StreamPartition>> sp = rs.get(spTag);
        PCollection<Map<StreamPartition, StreamSortSpec>> sss = rs.get(sssTag);
        PCollection<Map<StreamPartition, List<StreamRouterSpec>>> srs = rs.get(srsTag);

        PCollectionView<SpoutSpec> specView = spoutSpec.apply(View.asSingleton());
        PCollectionView<RouterSpec> routerView = routerSpec.apply(View.asSingleton());
        PCollectionView<PublishSpec> publishSpecView = publishSpec.apply(View.asSingleton());
        PCollectionView<Map<String, StreamDefinition>> sdsView = sds.apply(View.asSingleton());
        PCollectionView<List<StreamPartition>> spView = sp.apply(View.asSingleton());
        PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView = sss.apply(View.asSingleton());
        PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView = srs.apply(View.asSingleton());
        PCollectionView<AlertBoltSpec> alertBoltSpecView = alertBoltSpec.apply(View.asSingleton());

        TupleTag<PartitionedEvent> needWindow = new TupleTag<PartitionedEvent>(
                "needWindow") {

        };
        TupleTag<PartitionedEvent> noneedWindow = new TupleTag<PartitionedEvent>(
                "noneedWindow") {

        };
        PCollectionList<KV<Integer, PartitionedEvent>> parts = rawMessage.apply(new CorrelationSpoutFunction(specView, sdsView, numOfRouterBolts));
        List<StreamPartition> sps = Lists.newArrayList(SpecFactory.createRouterSpec().makeSSS().keySet());
        for (int i = 0; i < numOfRouterBolts; i++) {
            PCollection<KV<Integer, PartitionedEvent>> partition = parts.get(i);
            PCollectionTuple output = partition.apply(Values.create())
                    .apply("Find need handle",
                            new FindNeedWindowEventFunction(routerView, sdsView, sssView, srsView));
            // output.get(noneedWindow).apply("print1", ParDo.of(new FindNeedHandleEventTest.PrintinDoFn1()));
            PCollectionList<KV<Integer, PartitionedEvent>> pevents = output.get(needWindow)
                    .apply("covert to (streampartition->pevent)", new StreamPartitionFunction(spView, sps.size()));


            for (int j = 0; j < sps.size(); i++) {
                PCollection<KV<Integer, PartitionedEvent>> peventInPart = pevents.get(i);
                StreamPartition streamPartition = sps.get(i);
                String period = streamPartition.getSortSpec().getWindowPeriod();
                Duration flush = Duration.parse(period).plus(streamPartition.getSortSpec().getWindowMargin());
                PCollection<KV<Integer, PartitionedEvent>> windowedPevents = peventInPart.apply(
                        Window.<KV<Integer, PartitionedEvent>>into(FixedWindows.of(Duration.parse(period))).triggering(
                                AfterWatermark.pastEndOfWindow().withEarlyFirings(
                                        AfterProcessingTime.pastFirstElementInPane()
                                                .plusDelayOf(flush))).discardingFiredPanes()
                                .withAllowedLateness(Duration.ZERO));
                windowedPevents.apply(GroupByKey.create()).apply(Values.create())
                        .apply("alert bolt",new AlertBoltFunction(alertBoltSpecView, sdsView))
                        .apply("group by key", GroupByKey.create())
                        .apply("publish",
                        new AlertPublisherFunction(publishSpecView, alertBoltSpecView, sdsView, ConfigFactory.load()));
            }
        }

    }
}
