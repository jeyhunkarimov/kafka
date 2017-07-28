/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.streams.kstream;

/**
 * The {@code ValueJoinerWithKey} interface for joining two values into a new value of arbitrary type given read-only
 * key. Note that provided keys are read-only and should not be modified. Any key modification can result in corrupt
 * partitioning.
 * This is a stateless operation, i.e, {@link #apply(Object, Object, Object)} is invoked individually for each joining
 * record-pair of a {@link KStream}-{@link KStream}, {@link KStream}-{@link KTable}, or {@link KTable}-{@link KTable}
 * join.
 *
 * @param <K> key type
 * @param <V1> first value type
 * @param <V2> second value type
 * @param <VR> joined value type
 * @see KStream#join(KStream, ValueJoinerWithKey, JoinWindows)
 * @see KStream#join(KStream, ValueJoinerWithKey, JoinWindows, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde)
 * @see KStream#leftJoin(KStream, ValueJoinerWithKey, JoinWindows)
 * @see KStream#leftJoin(KStream, ValueJoinerWithKey, JoinWindows, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde)
 * @see KStream#outerJoin(KStream, ValueJoinerWithKey, JoinWindows)
 * @see KStream#outerJoin(KStream, ValueJoinerWithKey, JoinWindows, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde)
 * @see KStream#join(KTable, ValueJoinerWithKey)
 * @see KStream#join(KTable, ValueJoinerWithKey, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde)
 * @see KStream#leftJoin(KTable, ValueJoinerWithKey)
 * @see KStream#leftJoin(KTable, ValueJoinerWithKey, org.apache.kafka.common.serialization.Serde, org.apache.kafka.common.serialization.Serde)
 * @see KTable#join(KTable, ValueJoinerWithKey)
 * @see KTable#leftJoin(KTable, ValueJoinerWithKey)
 * @see KTable#outerJoin(KTable, ValueJoinerWithKey)
 */
public interface ValueJoinerWithKey<K, V1, V2, VR> {

    /**
     * Return a joined value consisting of {@code value1} and {@code value2} with given read-only {@code key}. Any
     * modification to {@code key} can result in corrupt partitioning.
     *
     * @param key the read-only key of particular record.
     * @param value1 the first value for joining
     * @param value2 the second value for joining
     * @return the joined value
     */
    VR apply(final K key, final V1 value1, final V2 value2);
}