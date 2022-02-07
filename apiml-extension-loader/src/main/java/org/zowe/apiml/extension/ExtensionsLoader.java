/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/
package org.zowe.apiml.extension;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Loader of Gateway extensions
*
*/
@Slf4j
@RequiredArgsConstructor
public class ExtensionsLoader implements ApplicationListener<ApplicationContextInitializedEvent> {

    @NonNull
    private final ExtensionConfigReader configReader;

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        log.info("on ApplicationContextInitializedEvent");
        if (!(event.getApplicationContext() instanceof BeanDefinitionRegistry)) {
            log.error("Expected Spring context to be a BeanDefinitionRegistry. Exiting Gateway Service");
            SpringApplication.exit(event.getApplicationContext(), () -> 1);
        } else {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) event.getApplicationContext();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);

            scanner.scan(configReader.getBasePackages());

            String[] beanNames = scanner.getRegistry().getBeanDefinitionNames();
            for (String name : beanNames) {
                if (!registry.containsBeanDefinition(name)) {
                    registry.registerBeanDefinition(name, scanner.getRegistry().getBeanDefinition(name));
                } else {
                    log.info("Bean with name " + name + " is already registered in the context");
                }
            }
        }
    }
}