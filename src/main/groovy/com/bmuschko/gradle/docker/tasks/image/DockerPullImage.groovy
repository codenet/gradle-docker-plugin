/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.docker.tasks.image

import com.bmuschko.gradle.docker.DockerRegistryCredentials
import com.bmuschko.gradle.docker.tasks.AbstractDockerRemoteApiTask
import com.bmuschko.gradle.docker.tasks.RegistryCredentialsAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import java.util.concurrent.Callable

class DockerPullImage extends AbstractDockerRemoteApiTask implements RegistryCredentialsAware {
    /**
     * The image repository.
     */
    @Input
    final Property<String> repository = project.objects.property(String)

    /**
     * The image's tag.
     */
    @Input
    @Optional
    final Property<String> tag = project.objects.property(String)

    /**
     * The target Docker registry credentials for pushing image.
     */
    @Nested
    @Optional
    DockerRegistryCredentials registryCredentials

    @Override
    void runRemoteCommand(dockerClient) {
        logger.quiet "Pulling repository '${repository.get()}'."
        def pullImageCmd = dockerClient.pullImageCmd(repository.get())

        if(tag.getOrNull()) {
            pullImageCmd.withTag(tag.get())
        }

        if(registryCredentials) {
            def authConfig = threadContextClassLoader.createAuthConfig(registryCredentials)
            pullImageCmd.withAuthConfig(authConfig)
        }

        def response = pullImageCmd.exec(threadContextClassLoader.createPullImageResultCallback(nextHandler))
        response.awaitSuccess()
    }

    @Internal
    Provider<String> getImageId() {
        project.provider(new Callable<String>() {
            @Override
            String call() throws Exception {
                tag.getOrNull()?.trim() ? "${repository.get()}:${tag.get()}" : repository.get()
            }
        })
    }
}
