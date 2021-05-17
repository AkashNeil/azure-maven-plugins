/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver.model;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class SqlServerEntity implements IAzureResourceEntity {

    private String name;
    private String id;
    private String resourceGroup;
    private String subscriptionId;
    private Region region;

    /**
     * TODO(qianjin): for type, state and kind, give the possible values in javadoc
     */
    private String kind;
    private String administratorLoginName;
    private String version;
    private String state;
    private String fullyQualifiedDomainName;
    private String type;

    private boolean enableAccessFromAzureServices;
    private boolean enableAccessFromLocalMachine;

}
