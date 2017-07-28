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
package org.apache.kafka.streams.kstream.internals;

import org.apache.kafka.common.internals.Topic;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.kstream.ValueJoinerWithKey;
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier;
import org.apache.kafka.streams.kstream.ValueTransformerSupplier;
import org.apache.kafka.streams.kstream.ReducerWithKey;
import org.apache.kafka.streams.kstream.Reducer;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.ValueMapperWithKey;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.kstream.InitializerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.kstream.Windows;
import org.apache.kafka.streams.processor.StateStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractStream<K> {

    protected final KStreamBuilder topology;
    protected final String name;
    protected final Set<String> sourceNodes;

    // This copy-constructor will allow to extend KStream
    // and KTable APIs with new methods without impacting the public interface.
    public AbstractStream(AbstractStream<K> stream) {
        this.topology = stream.topology;
        this.name = stream.name;
        this.sourceNodes = stream.sourceNodes;
    }

    AbstractStream(final KStreamBuilder topology, String name, final Set<String> sourceNodes) {
        if (sourceNodes == null || sourceNodes.isEmpty()) {
            throw new IllegalArgumentException("parameter <sourceNodes> must not be null or empty");
        }

        this.topology = topology;
        this.name = name;
        this.sourceNodes = sourceNodes;
    }


    Set<String> ensureJoinableWith(final AbstractStream<K> other) {
        Set<String> allSourceNodes = new HashSet<>();
        allSourceNodes.addAll(sourceNodes);
        allSourceNodes.addAll(other.sourceNodes);

        topology.copartitionSources(allSourceNodes);

        return allSourceNodes;
    }

    String getOrCreateName(final String queryableStoreName, final String prefix) {
        final String returnName = queryableStoreName != null ? queryableStoreName : topology.newStoreName(prefix);
        Topic.validate(returnName);
        return returnName;
    }

    static <T2, T1, R> ValueJoiner<T2, T1, R> reverseJoiner(final ValueJoiner<T1, T2, R> joiner) {
        return new ValueJoiner<T2, T1, R>() {
            @Override
            public R apply(T2 value2, T1 value1) {
                return joiner.apply(value1, value2);
            }
        };
    }

    static <K, T2, T1, R> ValueJoinerWithKey<K, T2, T1, R> reverseJoiner(final ValueJoinerWithKey<K, T1, T2, R> joinerWithKey) {
        return new ValueJoinerWithKey<K, T2, T1, R>() {
            @Override
            public R apply(K key, T2 value2, T1 value1) {
                return joinerWithKey.apply(key, value1, value2);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T, K>  StateStoreSupplier<KeyValueStore> keyValueStore(final Serde<K> keySerde,
                                                                   final Serde<T> aggValueSerde,
                                                                   final String storeName) {
        Objects.requireNonNull(storeName, "storeName can't be null");
        Topic.validate(storeName);
        return storeFactory(keySerde, aggValueSerde, storeName).build();
    }

    @SuppressWarnings("unchecked")
    static  <W extends Window, T, K> StateStoreSupplier<WindowStore> windowedStore(final Serde<K> keySerde,
                                                                                   final Serde<T> aggValSerde,
                                                                                   final Windows<W> windows,
                                                                                   final String storeName) {
        Objects.requireNonNull(storeName, "storeName can't be null");
        Topic.validate(storeName);
        return storeFactory(keySerde, aggValSerde, storeName)
                .windowed(windows.size(), windows.maintainMs(), windows.segments, false)
                .build();
    }

    static  <T, K> Stores.PersistentKeyValueFactory<K, T> storeFactory(final Serde<K> keySerde,
                                                                       final Serde<T> aggValueSerde,
                                                                       final String storeName) {
        return Stores.create(storeName)
                .withKeys(keySerde)
                .withValues(aggValueSerde)
                .persistent()
                .enableCaching();
    }

    static <K, V, VR> ValueMapperWithKey<K, V, VR> withKey(final ValueMapper<V, VR> valueMapper) {
        Objects.requireNonNull(valueMapper);
        return new ValueMapperWithKey<K, V, VR>() {
            @Override
            public VR apply(K key, V value) {
                return valueMapper.apply(value);
            }
        };
    }

    static <K, V1, V2, VR> ValueJoinerWithKey<K, V1, V2, VR> withKey(final ValueJoiner<V1, V2, VR> valueJoiner) {
        Objects.requireNonNull(valueJoiner);
        return new ValueJoinerWithKey<K, V1, V2, VR>() {
            @Override
            public VR apply(K key, V1 value1, V2 value2) {
                return valueJoiner.apply(value1, value2);
            }
        };
    }

    static <K, V, VR> ValueTransformerWithKey<K, V, VR> withKey(final ValueTransformer<V, VR> valueTransformer) {
        Objects.requireNonNull(valueTransformer);
        return new ValueTransformerWithKey<K, V, VR>() {
            @Override
            public void init(ProcessorContext context) {
                valueTransformer.init(context);
            }

            @Override
            public VR transform(K key, V value) {
                return valueTransformer.transform(value);
            }

            @Override
            public VR punctuate(long timestamp) {
                return valueTransformer.punctuate(timestamp);
            }

            @Override
            public void close() {
                valueTransformer.close();
            }
        };
    }

    static <K, V, VR> ValueTransformerWithKeySupplier<K, V, VR> withKey(final ValueTransformerSupplier<V, VR> valueTransformerSupplier) {
        Objects.requireNonNull(valueTransformerSupplier);
        return new ValueTransformerWithKeySupplier<K, V, VR>() {
            @Override
            public ValueTransformerWithKey<K, V, VR> get() {
                return withKey(valueTransformerSupplier.get());
            }
        };
    }

    static <K, V> ReducerWithKey<K, V> withKey(final Reducer<V> reducer) {
        Objects.requireNonNull(reducer);
        return new ReducerWithKey<K, V>() {
            @Override
            public V apply(K key, V value1, V value2) {
                return reducer.apply(value1, value2);
            }
        };
    }

    static <K, VA> InitializerWithKey<K, VA> withKey(final Initializer<VA> initializer) {
        Objects.requireNonNull(initializer);
        return new InitializerWithKey<K, VA>() {
            @Override
            public VA apply(K key) {
                return initializer.apply();
            }
        };
    }
}