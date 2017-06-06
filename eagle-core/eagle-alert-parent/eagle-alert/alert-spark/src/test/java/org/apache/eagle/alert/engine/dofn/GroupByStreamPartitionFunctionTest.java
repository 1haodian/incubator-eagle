package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class GroupByStreamPartitionFunctionTest {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testGroupByStreamPartitionFunction() {
    RouterSpec routerSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamRouterBoltSpec.json"),
            RouterSpec.class);
    PCollectionView<RouterSpec> routerSpecView = p.apply("getRouterSpec", Create.of(routerSpec))
        .apply(View.asSingleton());
    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();//"StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]"
    StreamSortSpec streamSortSpec = new StreamSortSpec();
    streamSortSpec.setWindowMargin(1000);
    streamSortSpec.setWindowPeriod("PT4S");
    streamPartition.setStreamId("oozieStream");
    streamPartition.setType(StreamPartition.Type.GROUPBY);
    streamPartition.setColumns(Lists.newArrayList("operation"));
    streamPartition.setSortSpec(streamSortSpec);
    pevent1.setEvent(new StreamEvent());
    pevent1.getEvent().setTimestamp(1);
    pevent1.setPartition(streamPartition);

    PartitionedEvent pevent2 = new PartitionedEvent();
    StreamPartition streamPartition2 = new StreamPartition();//"StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]"
    StreamSortSpec streamSortSpec2 = new StreamSortSpec();
    streamSortSpec2.setWindowMargin(2000);
    streamSortSpec2.setWindowPeriod("PT5S");
    streamPartition2.setStreamId("oozieStream");
    streamPartition2.setType(StreamPartition.Type.GROUPBY);
    streamPartition2.setColumns(Lists.newArrayList("operation"));
    streamPartition2.setSortSpec(streamSortSpec2);
    pevent2.setEvent(new StreamEvent());
    pevent2.getEvent().setTimestamp(13);
    pevent2.setPartition(streamPartition2);

    List<PartitionedEvent> pevents = Arrays.asList(pevent1, pevent2);
    List<String> keys = Lists.newArrayList(
        "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]",
    "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT5S,windowMargin=2000]]]");
        PCollection<PartitionedEvent> input = p.apply("create pevent", Create.of(pevents));

    int partitionNum = routerSpec.makeSSS().keySet().size();
    PCollectionList<KV<String, PartitionedEvent>> rs = input
        .apply("covert to streampartition->pevent",
            new GroupByStreamPartitionFunction(partitionNum));
    PCollection<KV<String, PartitionedEvent>> partition = rs.get(0);
    PAssert.that(partition).containsInAnyOrder(KV.of(keys.get(0), pevent1));
     /* PAssert.that(partition).satisfies(
          (SerializableFunction<Iterable<KV<String, Iterable<PartitionedEvent>>>, Void>) input1 -> {
            for (KV<String, Iterable<PartitionedEvent>> anInput1 : input1) {
              Assert.assertTrue(keys.contains(anInput1.getKey()));
              Iterator<PartitionedEvent> itr = anInput1.getValue().iterator();
              while (itr.hasNext()) {
                pevents.contains(itr.next());
              }
            }
            return null;
          });*/
    partition.apply("PrintinDoFn" + 0, ParDo.of(new PrintinDoFn(0)));

    PCollection<KV<String, PartitionedEvent>> partition1 = rs.get(1);

    PAssert.that(partition1).containsInAnyOrder((KV.of(keys.get(1), pevent2)));
    partition1.apply("PrintinDoFn" + 1, ParDo.of(new PrintinDoFn(1)));

    p.run();
  }

  private static class PrintinDoFn extends DoFn<KV<String, PartitionedEvent>, String> {

    private int partNum;

    public PrintinDoFn(int partNum) {
      this.partNum = partNum;
    }

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println("PrintinDoFn" + partNum + "   " + c.element());
    }
  }

}
