/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.distributed.proxy.AbstractBackend;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.*;
import org.infinispan.commons.CacheException;
import org.infinispan.functional.EntryView;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The extension of Bucket4j library addressed to support <a href="https://infinispan.org/">Infinispan</a> in-memory computing platform.
 */
public class InfinispanBackend<K extends Serializable> extends AbstractBackend<K> {

    private final ReadWriteMap<K, RemoteBucketState> readWriteMap;

    // TODO javadocs
    public InfinispanBackend(ReadWriteMap<K, RemoteBucketState> readWriteMap) {
        this.readWriteMap = Objects.requireNonNull(readWriteMap);
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, RemoteCommand<T> command) {
        InfinispanProcessor<K, T> entryProcessor = new InfinispanProcessor<>(command);
        try {
            return readWriteMap.eval(key, entryProcessor).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T extends Serializable> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command) {
        InfinispanProcessor<K, T> entryProcessor = new InfinispanProcessor<>(command);
        return readWriteMap.eval(key, entryProcessor);
    }


    public static class InfinispanEntry<K extends Serializable> implements MutableBucketEntry {

        private final EntryView.ReadWriteEntryView<K, RemoteBucketState> entryView;

        public InfinispanEntry(EntryView.ReadWriteEntryView<K, RemoteBucketState> entryView) {
            this.entryView = entryView;
        }

        @Override
        public RemoteBucketState get() {
            RemoteBucketState sourceState = entryView.get();
            return sourceState.deepCopy();
        }

        @Override
        public boolean exists() {
            return entryView.find().isPresent();
        }

        @Override
        public void set(RemoteBucketState value) {
            entryView.set(value);
        }

    }

}
