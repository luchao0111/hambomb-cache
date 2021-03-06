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
package org.hambomb.cache.loader;

import org.hambomb.cache.storage.key.KeyPermutationCombinationStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: <a herf="matilto:jarodchao@126.com>jarod </a>
 * @date: 2019-02-26
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cachekey {


    /**
     * 实体的唯一标识--Id或者复合主键
     * @return
     */
    String[] primaryKeys() default {"id"};

    /**
     * 查询实体用到的字段名
     * @return
     */
    String[] findKeys();


    KeyPermutationCombinationStrategy strategy() default KeyPermutationCombinationStrategy.COMBINATION;

    /** default values is findKeys().length */
    int peek() default 0;
}
