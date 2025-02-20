/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.DeploymentSlotBase;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.appservice.models.WebDeploymentSlotBasic;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WebAppDeploymentSlot extends AbstractAppService<DeploymentSlot, WebAppDeploymentSlotEntity> implements IWebAppDeploymentSlot {
    private static final String WEB_APP_DEPLOYMENT_SLOT_ID_TEMPLATE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s/slots/%s";
    @Nonnull
    private final IWebApp parent;
    @Nonnull
    private final WebApp webAppClient;

    public WebAppDeploymentSlot(@Nonnull final String id, @Nonnull final AppServiceManager azureClient) {
        super(id);
        this.webAppClient = azureClient.webApps().getById(ResourceId.fromString(id).parent().id());
        this.parent = Azure.az(AzureAppService.class).webapp(subscriptionId, resourceGroup, webAppClient.name());
    }

    public WebAppDeploymentSlot(@Nonnull final IWebApp parent, @Nonnull final WebApp webAppClient, @Nonnull final String slotName) {
        super(Utils.getSubscriptionId(webAppClient.id()), webAppClient.resourceGroupName(), slotName);
        this.webAppClient = webAppClient;
        this.parent = parent;
    }

    public WebAppDeploymentSlot(@Nonnull final IWebApp parent, @Nonnull final WebApp webAppClient, @Nonnull final WebDeploymentSlotBasic slotBasic) {
        super(slotBasic);
        this.webAppClient = webAppClient;
        this.parent = parent;
    }

    @Override
    public IWebApp webApp() {
        return parent;
    }

    @Override
    public Creator create() {
        return new WebAppDeploymentSlotCreator();
    }

    @Override
    public Updater update() {
        return new WebAppDeploymentSlotUpdater();
    }

    @NotNull
    @Override
    protected WebAppDeploymentSlotEntity getEntityFromRemoteResource(@NotNull DeploymentSlot remote) {
        return AppServiceUtils.fromWebAppDeploymentSlot(remote);
    }

    @Override
    protected DeploymentSlot loadRemote() {
        return webAppClient.deploymentSlots().getByName(name);
    }

    @Override
    public void delete() {
        remote().parent().deploymentSlots().deleteById(this.id());
        webApp().refresh();
    }

    @Override
    public void deploy(DeployType deployType, File targetFile, String targetPath) {
        final DeployOptions options = new DeployOptions().withPath(targetPath);
        remote().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), targetFile, options);
    }

    @Override
    public String id() {
        return String.format(WEB_APP_DEPLOYMENT_SLOT_ID_TEMPLATE, subscriptionId, resourceGroup, webAppClient.name(), name);
    }

    @Getter
    public class WebAppDeploymentSlotCreator implements Creator {
        public static final String CONFIGURATION_SOURCE_NEW = "new";
        public static final String CONFIGURATION_SOURCE_PARENT = "parent";
        private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";
        private static final String FAILED_TO_GET_CONFIGURATION_SOURCE = "Failed to get configuration source slot";

        private String name;
        private String configurationSource = CONFIGURATION_SOURCE_PARENT;
        private Map<String, String> appSettings = null;
        private DiagnosticConfig diagnosticConfig = null;

        @Override
        public Creator withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Creator withAppSettings(Map<String, String> appSettings) {
            this.appSettings = appSettings;
            return this;
        }

        @Override
        public Creator withConfigurationSource(String configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        @Override
        public Creator withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        @Override
        public WebAppDeploymentSlot commit() {
            final WebApp webApp = WebAppDeploymentSlot.this.webAppClient;
            final DeploymentSlot.DefinitionStages.Blank blank = webApp.deploymentSlots().define(getName());
            final DeploymentSlot.DefinitionStages.WithCreate withCreate;
            // Using configuration from parent by default
            final String source = StringUtils.isEmpty(configurationSource) ? CONFIGURATION_SOURCE_PARENT : StringUtils.lowerCase(configurationSource);
            switch (source) {
                case CONFIGURATION_SOURCE_NEW:
                    withCreate = blank.withBrandNewConfiguration();
                    break;
                case CONFIGURATION_SOURCE_PARENT:
                    withCreate = blank.withConfigurationFromParent();
                    break;
                default:
                    try {
                        final DeploymentSlot deploymentSlot = Optional.ofNullable(webApp.deploymentSlots().getByName(configurationSource))
                                .orElseThrow(() -> new AzureToolkitRuntimeException(CONFIGURATION_SOURCE_DOES_NOT_EXISTS));
                        withCreate = blank.withConfigurationFromDeploymentSlot(deploymentSlot);
                    } catch (ManagementException e) {
                        throw new AzureToolkitRuntimeException(FAILED_TO_GET_CONFIGURATION_SOURCE, e);
                    }
                    break;
            }
            if (getAppSettings() != null) {
                withCreate.withAppSettings(getAppSettings());
            }
            if (getDiagnosticConfig() != null) {
                AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, getDiagnosticConfig());
            }
            WebAppDeploymentSlot.this.remote = withCreate.create();
            WebAppDeploymentSlot.this.entity = AppServiceUtils.fromWebAppDeploymentSlot(WebAppDeploymentSlot.this.remote);
            WebAppDeploymentSlot.this.refreshStatus();
            WebAppDeploymentSlot.this.parent.refresh();
            return WebAppDeploymentSlot.this;
        }
    }

    @Getter
    private class WebAppDeploymentSlotUpdater implements Updater {
        private final List<String> appSettingsToRemove = new ArrayList<>();
        private final Map<String, String> appSettingsToAdd = new HashMap<>();
        private DiagnosticConfig diagnosticConfig = null;

        @Override
        public Updater withoutAppSettings(String key) {
            this.appSettingsToRemove.add(key);
            return this;
        }

        @Override
        public WebAppDeploymentSlotUpdater withAppSettings(Map<String, String> appSettings) {
            this.appSettingsToAdd.putAll(appSettings);
            return this;
        }

        @Override
        public WebAppDeploymentSlotUpdater withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        @Override
        public WebAppDeploymentSlot commit() {
            final DeploymentSlotBase.Update<DeploymentSlot> update = remote().update();
            if (getAppSettingsToAdd() != null) {
                update.withAppSettings(getAppSettingsToAdd());
            }
            if (getAppSettingsToRemove() != null) {
                getAppSettingsToRemove().forEach(update::withoutAppSetting);
            }
            if (getDiagnosticConfig() != null) {
                AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, getDiagnosticConfig());
            }
            WebAppDeploymentSlot.this.remote = update.apply();
            WebAppDeploymentSlot.this.entity = AppServiceUtils.fromWebAppDeploymentSlot(WebAppDeploymentSlot.this.remote);
            WebAppDeploymentSlot.this.refreshStatus();
            return WebAppDeploymentSlot.this;
        }
    }
}
