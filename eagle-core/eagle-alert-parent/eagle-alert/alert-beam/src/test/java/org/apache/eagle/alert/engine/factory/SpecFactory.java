package org.apache.eagle.alert.engine.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.PolicyDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;

import java.util.Map;

public class SpecFactory {

    public static PublishSpec createPublishSpec() {
        PublishSpec publishSpec = MetadataSerDeser
                .deserialize(SpecFactory.class.getResourceAsStream("/beam/testPublishSpec.json"),
                        PublishSpec.class);
        return publishSpec;
    }

    public static RouterSpec createRouterSpec() {
        RouterSpec routerSpec = MetadataSerDeser
                .deserialize(SpecFactory.class.getResourceAsStream("/beam/testStreamRouterBoltSpec.json"),
                        RouterSpec.class);
        return routerSpec;
    }

    public static SpoutSpec createSpoutSpec() {
        SpoutSpec spoutSpec = MetadataSerDeser
                .deserialize(SpecFactory.class.getResourceAsStream("/beam/testSpoutSpec.json"),
                        SpoutSpec.class);
        return spoutSpec;
    }

    public static AlertBoltSpec createAlertSpec() {
        AlertBoltSpec alertBoltSpec = MetadataSerDeser
                .deserialize(SpecFactory.class.getResourceAsStream("/beam/testAlertBoltSpec.json"),
                        AlertBoltSpec.class);

        alertBoltSpec.addPublishPartition("testAlertStream", "policy4", "file-testAlertStream",
                ImmutableSet.of("operation"));
        alertBoltSpec.addPublishPartition("testAlertStream", "policy5", "file-testAlertStream",
                ImmutableSet.of("operation"));
        PolicyDefinition policyDefinition1 = MetadataSerDeser
                .deserialize(SpecFactory.class.getResourceAsStream("/beam/testPolicy1.json"),
                        PolicyDefinition.class);
        PolicyDefinition policyDefinition2 = MetadataSerDeser
                .deserialize(SpecFactory.class.getResourceAsStream("/beam/testPolicy2.json"),
                        PolicyDefinition.class);
        alertBoltSpec.getBoltPoliciesMap().put("alertbolt1", Lists.newArrayList(policyDefinition1));
        String policyName1 = policyDefinition1.getName();
        alertBoltSpec.addBoltPolicy("alertbolt1", policyName1);
        String policyName2 = policyDefinition1.getName();
        alertBoltSpec.addBoltPolicy("alertbolt2", policyName2);
        alertBoltSpec.getBoltPoliciesMap().put("alertbolt2", Lists.newArrayList(policyDefinition2));
        return alertBoltSpec;
    }

    public static Map<String, StreamDefinition> createSds() {
        Map<String, StreamDefinition> sds = MetadataSerDeser
                .deserialize(SpecFactory.class.getResourceAsStream("/beam/testStreamDefinitionsSpec.json"),
                        new TypeReference<Map<String, StreamDefinition>>() {

                        });
        return sds;
    }
}