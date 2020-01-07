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

package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.Nothing;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;
import java.io.Serializable;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.*;

public class CommandResult<T> implements Serializable {

    private static final long serialVersionUID = 42;

    public static final CommandResult<Nothing> NOTHING = CommandResult.success(Nothing.INSTANCE, NULL_HANDLE);
    public static final CommandResult<Long> ZERO = CommandResult.success(0L, LONG_HANDLE);
    public static final CommandResult<Long> MAX_VALUE = CommandResult.success(Long.MAX_VALUE, LONG_HANDLE);
    public static final CommandResult<Boolean> TRUE = CommandResult.success(true, BOOLEAN_HANDLE);
    public static final CommandResult<Boolean> FALSE = CommandResult.success(false, BOOLEAN_HANDLE);

    private static final CommandResult<?> NOT_FOUND = new CommandResult<>(null, NULL_HANDLE.getTypeId(), true);
    private static final CommandResult<?> NULL = new CommandResult<>(null, NULL_HANDLE.getTypeId(), false);

    private T data;
    private int resultTypeId;
    private boolean bucketNotFound;

    public static SerializationHandle<CommandResult<?>> SERIALIZATION_HANDLE = new SerializationHandle<CommandResult<?>>() {
        @Override
        public <S> CommandResult<?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            boolean isBucketNotFound = adapter.readBoolean(input);
            if (isBucketNotFound) {
                return CommandResult.bucketNotFound();
            }
            int typeId = adapter.readInt(input);
            SerializationHandle handle = SerializationHandle.CORE_HANDLES.getHandleByTypeId(typeId);
            Serializable resultData = (Serializable) handle.deserialize(adapter, input);

            return CommandResult.success(resultData, typeId);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CommandResult<?> result) throws IOException {
            adapter.writeBoolean(output, result.bucketNotFound);
            if (!result.bucketNotFound) {
                adapter.writeInt(output, result.resultTypeId);
                SerializationHandle handle = SerializationHandle.CORE_HANDLES.getHandleByTypeId(result.resultTypeId);
                handle.serialize(adapter, output, result.data);
            }
        }

        @Override
        public int getTypeId() {
            return 14;
        }

        @Override
        public Class<CommandResult<?>> getSerializedType() {
            return (Class) CommandResult.class;
        }

    };

    public CommandResult(T data, int resultTypeId, boolean bucketNotFound) {
        this.data = data;
        this.resultTypeId = resultTypeId;
        this.bucketNotFound = bucketNotFound;
    }

    public static <R> CommandResult<R> success(R data, SerializationHandle dataSerializer) {
        return new CommandResult<>(data, dataSerializer.getTypeId(), false);
    }

    public static <R> CommandResult<R> success(R data, int resultTypeId) {
        return new CommandResult<>(data, resultTypeId, false);
    }

    public static <R> CommandResult<R> bucketNotFound() {
        return (CommandResult<R>) NOT_FOUND;
    }

    public static <R> CommandResult<R> empty() {
        return (CommandResult<R>) NULL;
    }

    public T getData() {
        if (bucketNotFound) {
            throw new IllegalStateException("getData must not be called when bucket not exists");
        }
        return data;
    }

    public boolean isBucketNotFound() {
        return bucketNotFound;
    }

}
