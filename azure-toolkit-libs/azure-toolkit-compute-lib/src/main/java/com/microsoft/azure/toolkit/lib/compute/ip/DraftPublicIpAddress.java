/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.ip;

import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DraftPublicIpAddress extends PublicIpAddress implements AzureResourceDraft<PublicIpAddress> {
    private Region region;
    private String leafDomainLabel;

    public DraftPublicIpAddress(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        super(getResourceId(subscriptionId, resourceGroup, name), null);
    }

    PublicIpAddress create(AzurePublicIpAddress module) {
        this.module = module;
        module.getPublicIpAddressManager(subscriptionId).define(name)
                .withRegion(region.getName())
                .withExistingResourceGroup(resourceGroup)
                .withLeafDomainLabel(leafDomainLabel)
                .create();
        refreshStatus();
        return this;
    }

    @Override
    protected String loadStatus() {
        return Optional.ofNullable(module).map(ignore -> super.loadStatus()).orElse(Status.DRAFT);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.network.models.PublicIpAddress loadRemote() {
        return Optional.ofNullable(module).map(ignore -> super.loadRemote()).orElse(null);
    }

    private static String getResourceId(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/publicIPAddresses/%s", subscriptionId, resourceGroup, name);
    }
}
