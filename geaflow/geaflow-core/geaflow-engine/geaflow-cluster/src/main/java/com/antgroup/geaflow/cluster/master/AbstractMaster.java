/*
 * Copyright 2023 AntGroup CO., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package com.antgroup.geaflow.cluster.master;

import com.antgroup.geaflow.cluster.clustermanager.ClusterContext;
import com.antgroup.geaflow.cluster.clustermanager.ClusterInfo;
import com.antgroup.geaflow.cluster.clustermanager.IClusterManager;
import com.antgroup.geaflow.cluster.common.AbstractComponent;
import com.antgroup.geaflow.cluster.heartbeat.HeartbeatManager;
import com.antgroup.geaflow.cluster.resourcemanager.DefaultResourceManager;
import com.antgroup.geaflow.cluster.resourcemanager.IResourceManager;
import com.antgroup.geaflow.cluster.resourcemanager.ResourceManagerContext;
import com.antgroup.geaflow.cluster.rpc.RpcAddress;
import com.antgroup.geaflow.cluster.rpc.impl.MasterEndpoint;
import com.antgroup.geaflow.cluster.rpc.impl.ResourceManagerEndpoint;
import com.antgroup.geaflow.cluster.rpc.impl.RpcServiceImpl;
import com.antgroup.geaflow.cluster.web.HttpServer;
import com.antgroup.geaflow.common.config.Configuration;
import com.antgroup.geaflow.common.config.keys.ExecutionConfigKeys;
import com.antgroup.geaflow.common.rpc.ConfigurableServerOption;
import com.antgroup.geaflow.common.utils.PortUtil;
import com.antgroup.geaflow.common.utils.ProcessUtil;
import com.antgroup.geaflow.ha.leaderelection.ILeaderContender;
import com.antgroup.geaflow.ha.leaderelection.ILeaderElectionService;
import com.antgroup.geaflow.ha.leaderelection.LeaderElectionServiceFactory;
import com.baidu.brpc.server.RpcServerOptions;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMaster extends AbstractComponent implements IMaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMaster.class);

    protected IResourceManager resourceManager;
    protected IClusterManager clusterManager;
    protected HeartbeatManager heartbeatManager;
    protected RpcAddress masterAddress;
    protected HttpServer httpServer;
    protected ClusterContext clusterContext;
    protected ILeaderElectionService leaderElectionService;

    public AbstractMaster() {
        this(0);
    }

    public AbstractMaster(int rpcPort) {
        super(rpcPort);
    }

    @Override
    public void init(MasterContext context) {
        super.init(context.getId(), context.getConfiguration().getMasterId(),
            context.getConfiguration());

        this.clusterManager = context.getClusterManager();
        this.clusterContext = context.getClusterContext();
        this.heartbeatManager = new HeartbeatManager(configuration, clusterManager);
        this.resourceManager = new DefaultResourceManager(clusterManager);
        this.clusterContext.setHeartbeatManager(heartbeatManager);

        initEnv(context);
    }

    protected void initEnv(MasterContext context) {
        this.clusterManager.init(clusterContext);
        startRpcService(clusterManager, resourceManager);

        // Register service info and initialize cluster.
        registerHAService();
        // Start container.
        resourceManager.init(ResourceManagerContext.build(context, clusterContext));

        if (configuration.getBoolean(ExecutionConfigKeys.HTTP_REST_SERVICE_ENABLE)) {
            httpServer = new HttpServer(configuration, clusterManager, heartbeatManager,
                resourceManager);
            httpServer.start();
        }
    }

    public void initLeaderElectionService(ILeaderContender contender,
                                          Configuration configuration,
                                          int componentId) {
        leaderElectionService = LeaderElectionServiceFactory.loadElectionService(configuration);
        leaderElectionService.init(configuration, String.valueOf(componentId));
        leaderElectionService.open(contender);
        LOGGER.info("Leader election service enabled for master.");
    }

    public void waitForLeaderElection() throws InterruptedException {
        LOGGER.info("Wait for becoming a leader...");
        synchronized (leaderElectionService) {
            leaderElectionService.wait();
        }
    }

    public void notifyLeaderElection() {
        synchronized (leaderElectionService) {
            leaderElectionService.notify();
        }
    }

    protected void startRpcService(IClusterManager clusterManager,
                                 IResourceManager resourceManager) {
        RpcServerOptions serverOptions = ConfigurableServerOption.build(configuration);
        int port = PortUtil.getPort(rpcPort);
        this.rpcService = new RpcServiceImpl(port, serverOptions);
        this.rpcService.addEndpoint(new MasterEndpoint(this, clusterManager));
        this.rpcService.addEndpoint(new ResourceManagerEndpoint(resourceManager));
        this.rpcPort = rpcService.startService();
        this.masterAddress = new RpcAddress(ProcessUtil.getHostIp(), port);
    }

    public ClusterInfo startCluster() {
        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setMasterAddress(masterAddress);
        Map<String, RpcAddress> driverAddresses = clusterManager.startDrivers();
        clusterInfo.setDriverAddresses(driverAddresses);
        LOGGER.info("init cluster with info: {}", clusterInfo);
        return clusterInfo;
    }

    @Override
    public void close() {
        super.close();
        clusterManager.close();
        if (heartbeatManager != null) {
            heartbeatManager.close();
        }
        if (httpServer != null) {
            httpServer.stop();
        }
        LOGGER.info("master {} closed", name);
    }

}

