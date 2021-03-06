/*
 * Copyright 2019 The  Project
 *
 * The   Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.hambomb.cache.handler;

/**
 * @author: <a herf="matilto:jarodchao@126.com>jarod </a>
 * @date: 2019-02-26
 */
public interface CacheHandler<T> {

    void put(String key, T value);

    T getByRealKey(String key);

    T getByIndexKey(String key);

    void update(String key, T value);

    void delete(String key);

    void load(String key, T value);

    String getRealKeyByIndexKey(String key);
}
