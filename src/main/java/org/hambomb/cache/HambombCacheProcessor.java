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
package org.hambomb.cache;

import org.hambomb.cache.cluster.ClusterProcessor;
import org.hambomb.cache.db.entity.CacheObjectMapper;
import org.hambomb.cache.db.entity.Cachekey;
import org.hambomb.cache.db.entity.EntityLoader;
import org.hambomb.cache.db.entity.MapperScanner;
import org.hambomb.cache.handler.CacheHandler;
import org.hambomb.cache.index.IndexFactory;
import org.hambomb.cache.storage.RedisKeyCcombinedStrategy;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.*;

/**
 * @author: <a herf="mailto:jarodchao@126.com>jarod </a>
 * @date: 2019-02-26
 */
public class HambombCacheProcessor implements ApplicationContextAware, InitializingBean {


    ApplicationContext applicationContext;

    Configuration configuration;

    private Map<String, EntityLoader> entityLoaderMap;

    private CacheHandler cacheHandler;

    private MapperScanner scanner;

    private Set<Class<? extends CacheObjectMapper>> mappers;

    private static final Logger LOG = LoggerFactory.getLogger(HambombCacheProcessor.class);

    @Autowired
    private ClusterProcessor clusterProcessor;

    public HambombCacheProcessor(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Boolean masterFlag = false;

        if (Configuration.CacheServerStrategy.CLUSTER == configuration.strategy) {

            clusterProcessor.initNodes();

            masterFlag = clusterProcessor.selectMasterLoader();

            clusterProcessor.initDataLoadNode();

        }

        if (!masterFlag) {
            LOG.info("Application Server not was a Master Node,HambombCacheProcessor is stopping.");
            return;
        }

        process();

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void process() {

        if (configuration.scanPackageName != null && !"".equals(configuration.scanPackageName)) {

            scanner = new MapperScanner(configuration.scanPackageName);

            mappers = scanner.scanMapper();
        }

        if (configuration.handler != null) {
            this.cacheHandler = configuration.handler;
        }

        List<EntityLoader> entityLoaders = buildLoaders(mappers);

        startLoader(entityLoaders);

        clusterProcessor.finishDataLoadNode();
    }

    private void startLoader(List<EntityLoader> entityLoaders) {

        entityLoaderMap = new HashMap<>(entityLoaders.size());

        for (EntityLoader entityLoader : entityLoaders) {
            entityLoader.loadEntities().stream().forEach(o -> {
                entityLoader.getPkey(o);
                entityLoader.getFKeys(o);
            });

            entityLoaderMap.put(entityLoader.indexFactory.uniqueKey, entityLoader);
        }
    }

    private List<EntityLoader> buildLoaders(Set<Class<? extends CacheObjectMapper>> mappers) {

        return mappers.parallelStream().map((Class<? extends CacheObjectMapper> aClass) -> {

            CacheObjectMapper mapper = null;

            if (applicationContext != null) {
                mapper = applicationContext.getBean(aClass);
            } else {
                try {
                    mapper = aClass.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            EntityLoader entityLoader = new EntityLoader(mapper);

            Class entityClass = mapper.getSubEntityClass();

            /** 取@的值 */
            Cachekey cachekey = (Cachekey) entityClass.getAnnotation(Cachekey.class);

            String[] pk = cachekey.primaryKeys();
            String[] fk = cachekey.findKeys();

            IndexFactory indexFactory = IndexFactory.create(mapper.getClass().getSimpleName(), pk, fk, new RedisKeyCcombinedStrategy());
            entityLoader.addIndexFactory(indexFactory);

            entityLoader.initializeLoader();

            for (String p : pk) {

                entityLoader.addPkGetter(getterMethod(p, entityClass));
            }

            for (String f : fk) {

                entityLoader.addFkGetter(getterMethod(f, entityClass));
            }

            return entityLoader;

        }).collect(Collectors.toList());

    }

    private Method getterMethod(String name, Class entityClazz) {
        Set<Method> getters = ReflectionUtils.getAllMethods(entityClazz,
                withModifier(Modifier.PUBLIC), withName(CacheUtils.getter(name)), withParametersCount(0));

        return getters.stream().findFirst().get();
    }
}