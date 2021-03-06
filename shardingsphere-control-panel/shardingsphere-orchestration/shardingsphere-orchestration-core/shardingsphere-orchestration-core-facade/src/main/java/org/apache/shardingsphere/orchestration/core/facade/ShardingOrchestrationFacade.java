/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.orchestration.core.facade;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.cluster.configuration.config.ClusterConfiguration;
import org.apache.shardingsphere.infra.auth.Authentication;
import org.apache.shardingsphere.metrics.configuration.config.MetricsConfiguration;
import org.apache.shardingsphere.orchestration.core.registry.RegistryCenter;
import org.apache.shardingsphere.orchestration.repository.api.ConfigCenterRepository;
import org.apache.shardingsphere.orchestration.repository.api.RegistryCenterRepository;
import org.apache.shardingsphere.orchestration.repository.api.config.CenterConfiguration;
import org.apache.shardingsphere.orchestration.repository.api.config.OrchestrationConfiguration;
import org.apache.shardingsphere.orchestration.core.common.CenterType;
import org.apache.shardingsphere.orchestration.core.config.ConfigCenter;
import org.apache.shardingsphere.orchestration.core.facade.listener.ShardingOrchestrationListenerManager;
import org.apache.shardingsphere.orchestration.core.facade.properties.OrchestrationProperties;
import org.apache.shardingsphere.orchestration.core.facade.properties.OrchestrationPropertyKey;
import org.apache.shardingsphere.orchestration.core.metadata.MetaDataCenter;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.spi.type.TypedSPIRegistry;
import org.apache.shardingsphere.infra.config.DataSourceConfiguration;
import org.apache.shardingsphere.infra.config.RuleConfiguration;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

/**
 * Sharding orchestration facade.
 */
@Slf4j
public final class ShardingOrchestrationFacade implements AutoCloseable {
    
    static {
        // TODO avoid multiple loading
        ShardingSphereServiceLoader.register(ConfigCenterRepository.class);
        ShardingSphereServiceLoader.register(RegistryCenterRepository.class);
    }
    
    private ConfigCenterRepository configCenterRepository;
    
    private RegistryCenterRepository registryCenterRepository;

    private ConfigCenterRepository centerRepository;
    
    @Getter
    private boolean isOverwrite;
    
    @Getter
    private ConfigCenter configCenter;
    
    @Getter
    private RegistryCenter registryCenter;

    @Getter
    private MetaDataCenter metaDataCenter;
    
    private ShardingOrchestrationListenerManager listenerManager;
    
    private String configCenterName;
    
    private String registryCenterName;
    
    private String metaDataCenterName;
    
    /**
     * Init orchestration facade.
     *
     * @param orchestrationConfig orchestration configuration
     * @param shardingSchemaNames collection of sharding schema names
     */
    public void init(final OrchestrationConfiguration orchestrationConfig, final Collection<String> shardingSchemaNames) {
        initConfigCenter(orchestrationConfig);
        initRegistryCenter(orchestrationConfig);
        initMetaDataCenter(orchestrationConfig);
        initListenerManager(shardingSchemaNames);
    }
    
    private void initConfigCenter(final OrchestrationConfiguration orchestrationConfig) {
        Optional<String> optional = getInstanceNameByOrchestrationType(orchestrationConfig.getInstanceConfigurationMap(), CenterType.CONFIG_CENTER.getValue());
        Preconditions.checkArgument(optional.isPresent(), "Can not find instance configuration with config center orchestration type.");
        configCenterName = optional.get();
        CenterConfiguration configCenterConfiguration = orchestrationConfig.getInstanceConfigurationMap().get(configCenterName);
        Preconditions.checkNotNull(configCenterConfiguration, "Config center configuration cannot be null.");
        configCenterRepository = TypedSPIRegistry.getRegisteredService(
                ConfigCenterRepository.class, configCenterConfiguration.getType(), configCenterConfiguration.getProps());
        configCenterRepository.init(configCenterConfiguration);
        isOverwrite = new OrchestrationProperties(configCenterConfiguration.getProps()).getValue(OrchestrationPropertyKey.OVERWRITE);
        configCenter = new ConfigCenter(configCenterName, configCenterRepository);
    }
    
    private void initRegistryCenter(final OrchestrationConfiguration orchestrationConfig) {
        Optional<String> optional = getInstanceNameByOrchestrationType(orchestrationConfig.getInstanceConfigurationMap(), CenterType.REGISTRY_CENTER.getValue());
        Preconditions.checkArgument(optional.isPresent(), "Can not find instance configuration with registry center orchestration type.");
        registryCenterName = optional.get();
        CenterConfiguration registryCenterConfiguration = orchestrationConfig.getInstanceConfigurationMap().get(registryCenterName);
        Preconditions.checkNotNull(registryCenterConfiguration, "Registry center configuration cannot be null.");
        registryCenterRepository = TypedSPIRegistry.getRegisteredService(RegistryCenterRepository.class, registryCenterConfiguration.getType(), registryCenterConfiguration.getProps());
        registryCenterRepository.init(registryCenterConfiguration);
        registryCenter = new RegistryCenter(registryCenterName, registryCenterRepository);
    }
    
    private void initMetaDataCenter(final OrchestrationConfiguration orchestrationConfig) {
        Optional<String> optional = getInstanceNameByOrchestrationType(orchestrationConfig.getInstanceConfigurationMap(), CenterType.METADATA_CENTER.getValue());
        Preconditions.checkArgument(optional.isPresent(), "Can not find instance configuration with metadata center orchestration type.");
        metaDataCenterName = optional.get();
        CenterConfiguration metaDataCenterConfiguration = orchestrationConfig.getInstanceConfigurationMap().get(metaDataCenterName);
        Preconditions.checkNotNull(metaDataCenterConfiguration, "MetaData center configuration cannot be null.");
        centerRepository = TypedSPIRegistry.getRegisteredService(ConfigCenterRepository.class, metaDataCenterConfiguration.getType(), metaDataCenterConfiguration.getProps());
        centerRepository.init(metaDataCenterConfiguration);
        metaDataCenter = new MetaDataCenter(metaDataCenterName, centerRepository);
    }
    
    private void initListenerManager(final Collection<String> shardingSchemaNames) {
        listenerManager = new ShardingOrchestrationListenerManager(
                registryCenterName, registryCenterRepository, configCenterName,
                configCenterRepository, metaDataCenterName, centerRepository,
                shardingSchemaNames.isEmpty() ? configCenter.getAllShardingSchemaNames() : shardingSchemaNames);
    }
    
    /**
     * Initialize configurations of orchestration.
     *
     * @param dataSourceConfigurationMap schema data source configuration map
     * @param schemaRuleMap schema rule map
     * @param authentication authentication
     * @param props properties
     */
    public void initConfigurations(final Map<String, Map<String, DataSourceConfiguration>> dataSourceConfigurationMap, 
                                   final Map<String, Collection<RuleConfiguration>> schemaRuleMap, final Authentication authentication, final Properties props) {
        configCenter.persistGlobalConfiguration(authentication, props, isOverwrite);
        for (Entry<String, Map<String, DataSourceConfiguration>> entry : dataSourceConfigurationMap.entrySet()) {
            configCenter.persistConfigurations(entry.getKey(), dataSourceConfigurationMap.get(entry.getKey()), schemaRuleMap.get(entry.getKey()), isOverwrite);
        }
        initConfigurations();
    }
    
    /**
     * Initialize configurations of orchestration.
     */
    public void initConfigurations() {
        registryCenter.persistInstanceOnline();
        registryCenter.persistDataSourcesNode();
        listenerManager.initListeners();
    }
    
    /**
     * Initialize metrics configuration to config center.
     *
     * @param metricsConfiguration metrics configuration
     */
    public void initMetricsConfiguration(final MetricsConfiguration metricsConfiguration) {
        configCenter.persistMetricsConfiguration(metricsConfiguration, isOverwrite);
    }
    
    /**
     * Initialize cluster configuration to config center.
     *
     * @param clusterConfiguration cluster configuration
     */
    public void initClusterConfiguration(final ClusterConfiguration clusterConfiguration) {
        configCenter.persistClusterConfiguration(clusterConfiguration, isOverwrite);
    }
    
    @Override
    public void close() {
        try {
            configCenterRepository.close();
            registryCenterRepository.close();
            centerRepository.close();
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.warn("RegCenter exception for: {}", ex.getMessage());
        }
    }
    
    private Optional<String> getInstanceNameByOrchestrationType(final Map<String, CenterConfiguration> map, final String type) {
        return (null == map || null == type) ? Optional.empty() : map.entrySet()
                .stream().filter(entry -> contains(entry.getValue().getOrchestrationType(), type)).findFirst().map(Map.Entry::getKey);
    }
    
    private boolean contains(final String collection, final String element) {
        return Splitter.on(",").omitEmptyStrings().trimResults().splitToList(collection).stream().anyMatch(each -> element.equals(each.trim()));
    }
    
    /**
     * Get orchestration facade instance.
     *
     * @return orchestration facade instance
     */
    public static ShardingOrchestrationFacade getInstance() {
        return ShardingOrchestrationFacadeHolder.INSTANCE;
    }
    
    private static final class ShardingOrchestrationFacadeHolder {
        
        public static final ShardingOrchestrationFacade INSTANCE = new ShardingOrchestrationFacade();
    }
}
