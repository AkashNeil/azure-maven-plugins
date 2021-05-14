/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import reactor.core.publisher.Flux;

public interface IAppService extends IResource, IKuduClient {
    void start();

    void stop();

    void restart();

    void delete();

    boolean exists();

    String hostName();

    String state();

    Runtime getRuntime();

    PublishingProfile getPublishingProfile();

    DiagnosticConfig getDiagnosticConfig();

    Flux<String> streamAllLogsAsync();
}
