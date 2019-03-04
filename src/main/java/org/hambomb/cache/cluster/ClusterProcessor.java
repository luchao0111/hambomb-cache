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
package org.hambomb.cache.cluster;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.hambomb.cache.cluster.node.CacheLoaderMaster;
import org.hambomb.cache.cluster.node.CacheMasterLoaderData;
import org.hambomb.cache.cluster.node.ClusterRoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author: <a herf="mailto:jarodchao@126.com>jarod </a>
 * @date: 2019-02-27
 */
public class ClusterProcessor {

    public ZkClient zkClient;

    private IZkDataListener cacheMasterListener;

    public ClusterProcessor(ZkClient zkClient) throws IOException {
        this.zkClient = zkClient;
    }

    public Boolean initNodes() {

        if (!zkClient.exists(ClusterRoot.getRootPath())) {
            zkClient.createPersistent(ClusterRoot.getRootPath());
        }

        if (!zkClient.exists(ClusterRoot.getDataPath())) {
            zkClient.createPersistent(ClusterRoot.getDataPath());
        }

        return true;
    }

    public Boolean selectMasterLoader() {

        zkClient.subscribeDataChanges(ClusterRoot.getMasterPath(),cacheMasterListener);

        if (!zkClient.exists(ClusterRoot.getMasterPath())) {
            zkClient.createEphemeral(ClusterRoot.getMasterPath(),new CacheLoaderMaster());

            return true;
        }

        return false;
    }

    public void finishDataLoadNode() {

        CacheMasterLoaderData cacheMasterLoaderData = zkClient.readData(ClusterRoot.getMasterData());

        cacheMasterLoaderData.finishDataLoad();

        zkClient.writeData(ClusterRoot.getMasterData(), cacheMasterLoaderData);

    }

    public void initDataLoadNode() {

        if (!zkClient.exists(ClusterRoot.getMasterData())) {

            CacheMasterLoaderData cacheMasterLoaderData = new CacheMasterLoaderData();

            zkClient.createPersistent(ClusterRoot.getMasterData(),cacheMasterLoaderData);
        }
    }
}
