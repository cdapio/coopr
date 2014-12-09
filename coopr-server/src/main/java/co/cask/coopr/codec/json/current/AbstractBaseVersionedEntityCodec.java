/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.codec.json.current;

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.BaseVersionedEntity;
import com.google.gson.*;

/**
 * Codec for serializing/deserializing a {@link BaseVersionedEntity}.
 * @param <T> type of base versioned entity to serialize and deserialize.
 */
public abstract class AbstractBaseVersionedEntityCodec<T extends BaseVersionedEntity>
        extends AbstractBaseEntityCodec<T> {

    @Override
    protected void addChildFields(T entity, JsonObject jsonObj, JsonSerializationContext context) {
        jsonObj.add("version", context.serialize(entity.getVersion()));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BaseEntity.Builder<T> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
        return builder(jsonObj, context)
                .setVersion(context.<Integer>deserialize(jsonObj.get("version"), Integer.class));
    }

    protected abstract BaseVersionedEntity.Builder<T> builder(JsonObject jsonObj, JsonDeserializationContext context);
}
