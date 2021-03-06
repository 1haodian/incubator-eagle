/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.alert.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamIdConversion {
    public static final String STREAM_ID_TEMPLATE = "stream_%s_to_%s";
    public static final String STREAM_ID_NUM_TEMPLATE = "stream_%s";
    private static final Pattern lastIntPattern = Pattern.compile("[^0-9]+([0-9]+)$");

    public static String generateStreamIdBetween(String sourceId, String targetId) {
        return String.format(STREAM_ID_TEMPLATE, sourceId, targetId);
    }

    /**
     * Hard-coded stream format in stream_${partitionNum}.
     */
    public static String generateStreamIdByPartition(int partitionNum) {
        return String.format(STREAM_ID_NUM_TEMPLATE, partitionNum);
    }

    public static int getPartitionNumByTargetId(String sourceIdTotargetId) {
        Matcher matcher = lastIntPattern.matcher(sourceIdTotargetId);
        if (matcher.find()) {
            String someNumberStr = matcher.group(1);
            return Integer.parseInt(someNumberStr);
        }
        return -1;
    }

}
