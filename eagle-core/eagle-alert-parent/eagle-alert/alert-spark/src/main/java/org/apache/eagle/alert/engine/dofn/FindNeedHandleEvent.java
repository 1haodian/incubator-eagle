package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.StreamRouterSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.List;
import java.util.Map;

public class FindNeedHandleEvent
    extends PTransform<PCollection<PartitionedEvent>, PCollectionTuple> {

  private PCollectionView<RouterSpec> routerSpecView;
  private PCollectionView<Map<String, StreamDefinition>> sdsView;
  private PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView;
  private PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView;
  private TupleTag<PartitionedEvent> peventNeedHandle = new TupleTag<PartitionedEvent>(
      "peventNeedHandle") {

  };
  private TupleTag<PartitionedEvent> peventNOTNeedHandle = new TupleTag<PartitionedEvent>(
      "peventNOTNeedHandle") {

  };

  public FindNeedHandleEvent(PCollectionView<RouterSpec> routerSpecView,
      PCollectionView<Map<String, StreamDefinition>> sdsView,
      PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView,
      PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView) {
    this.routerSpecView = routerSpecView;
    this.sdsView = sdsView;
    this.sssView = sssView;
    this.srsView = srsView;
  }

  @Override public PCollectionTuple expand(PCollection<PartitionedEvent> events) {

    return events.apply("division handle or no handle", ParDo
        .of(new DoFn<PartitionedEvent, PartitionedEvent>() {

          @ProcessElement public void processElement(ProcessContext c) {
            PartitionedEvent pevent = c.element();
              if (!dispatchToSortHandler(pevent)) {
                c.output(peventNOTNeedHandle, c.element());
              } else {
                c.output(peventNeedHandle, c.element());
              }
          }
        }).withOutputTags(peventNOTNeedHandle, TupleTagList.of(peventNeedHandle))
        .withSideInputs(routerSpecView, sdsView, sssView, srsView));
  }

  private boolean dispatchToSortHandler(PartitionedEvent event) {
    if (event.getTimestamp() <= 0) {
      return false;
    }

    /*StreamSortHandler sortHandler = streamSortHandlers.get(event.getPartition());
    if (sortHandler == null) {
      if (event.isSortRequired()) {
        LOG.warn("Stream sort handler required has not been loaded so emmit directly: {}", event);
        this.context.counter().incr("miss_sort_count");
      }
      return false;
    } else {
      sortHandler.nextEvent(event);
      return true;
    }*/
    return true;
  }

 /* .of(new DoFn<String, String>() {
    public void processElement(ProcessContext c) {
      String word = c.element();
      if (word.length() <= wordLengthCutOff) {
        // Emit short word to the main output.
        // In this example, it is the output with tag wordsBelowCutOffTag.
        c.output(word);
      } else {
        // Emit long word length to the output with tag wordLengthsAboveCutOffTag.
        c.output(wordLengthsAboveCutOffTag, word.length());
      }
      if (word.startsWith("MARKER")) {
        // Emit word to the output with tag markedWordsTag.
        c.output(markedWordsTag, word);
      }
    }}));

      .apply("AddEventTimestamps",
             WithTimestamps.of((GameActionInfo i) -> new Instant(i.getTimestamp())))
    */
}
