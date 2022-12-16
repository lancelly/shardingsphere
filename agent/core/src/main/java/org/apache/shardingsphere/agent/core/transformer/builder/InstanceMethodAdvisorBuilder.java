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

package org.apache.shardingsphere.agent.core.transformer.builder;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.shardingsphere.agent.config.advisor.InstanceMethodAdvisorConfiguration;
import org.apache.shardingsphere.agent.config.plugin.PluginConfiguration;
import org.apache.shardingsphere.agent.core.logging.LoggerFactory;
import org.apache.shardingsphere.agent.core.plugin.OverrideArgsInvoker;
import org.apache.shardingsphere.agent.core.plugin.advice.InstanceMethodAroundAdvice;
import org.apache.shardingsphere.agent.core.plugin.interceptor.InstanceMethodAroundInterceptor;
import org.apache.shardingsphere.agent.core.plugin.interceptor.InstanceMethodInterceptorArgsOverride;
import org.apache.shardingsphere.agent.core.plugin.interceptor.composed.ComposedInstanceMethodAroundInterceptor;
import org.apache.shardingsphere.agent.core.plugin.interceptor.composed.ComposedInstanceMethodInterceptorArgsOverride;
import org.apache.shardingsphere.agent.core.plugin.loader.AdviceInstanceLoader;
import org.apache.shardingsphere.agent.core.transformer.MethodAdvisor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Instance method advisor builder.
 */
@RequiredArgsConstructor
public final class InstanceMethodAdvisorBuilder {
    
    private static final LoggerFactory.Logger LOGGER = LoggerFactory.getLogger(InstanceMethodAdvisorBuilder.class);
    
    private final Map<String, PluginConfiguration> pluginConfigs;
    
    private final Collection<InstanceMethodAdvisorConfiguration> instanceMethodAdvisorConfigs;
    
    private final boolean isEnhancedForProxy;
    
    private final TypeDescription typePointcut;
    
    private final ClassLoader classLoader;
    
    /**
     * Create instance method advisor builder.
     * 
     * @param builder original builder
     * @return created builder
     */
    public Builder<?> create(final Builder<?> builder) {
        Collection<MethodAdvisor<?>> instanceMethodAdviceComposePoints = typePointcut.getDeclaredMethods().stream()
                .filter(each -> !(each.isAbstract() || each.isSynthetic()))
                .map(this::getMatchedInstanceMethodPoint)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Builder<?> result = builder;
        for (MethodAdvisor<?> each : instanceMethodAdviceComposePoints) {
            try {
                if (each.getAdvice() instanceof InstanceMethodInterceptorArgsOverride) {
                    result = result.method(ElementMatchers.is(each.getPointcut()))
                            .intercept(MethodDelegation.withDefaultConfiguration().withBinders(Morph.Binder.install(OverrideArgsInvoker.class)).to(each.getAdvice()));
                } else {
                    result = result.method(ElementMatchers.is(each.getPointcut())).intercept(MethodDelegation.withDefaultConfiguration().to(each.getAdvice()));
                }
                // CHECKSTYLE:OFF
            } catch (final Throwable ex) {
                // CHECKSTYLE:ON
                LOGGER.error("Failed to load advice class: {}.", typePointcut.getTypeName(), ex);
            }
        }
        return result;
    }
    
    private MethodAdvisor<?> getMatchedInstanceMethodPoint(final InDefinedShape methodPointcut) {
        List<InstanceMethodAdvisorConfiguration> instanceMethodAdvisorConfigs = this.instanceMethodAdvisorConfigs
                .stream().filter(each -> each.getPointcut().matches(methodPointcut)).collect(Collectors.toList());
        if (instanceMethodAdvisorConfigs.isEmpty()) {
            return null;
        }
        if (1 == instanceMethodAdvisorConfigs.size()) {
            return getSingleInstanceMethodPoint(methodPointcut, instanceMethodAdvisorConfigs.get(0));
        }
        return getComposeInstanceMethodPoint(methodPointcut);
    }
    
    private MethodAdvisor<?> getSingleInstanceMethodPoint(final InDefinedShape methodPointcut, final InstanceMethodAdvisorConfiguration instanceMethodAdvisorConfig) {
        InstanceMethodAroundAdvice instanceMethodAroundAdvice = loadAdviceInstance(instanceMethodAdvisorConfig.getAdviceClassName());
        return instanceMethodAdvisorConfig.isOverrideArgs()
                ? new MethodAdvisor<>(methodPointcut, new InstanceMethodInterceptorArgsOverride(instanceMethodAroundAdvice))
                : new MethodAdvisor<>(methodPointcut, new InstanceMethodAroundInterceptor(instanceMethodAroundAdvice));
    }
    
    private MethodAdvisor<?> getComposeInstanceMethodPoint(final InDefinedShape methodPointcut) {
        Collection<InstanceMethodAroundAdvice> instanceMethodAroundAdvices = new LinkedList<>();
        boolean isArgsOverride = false;
        for (InstanceMethodAdvisorConfiguration each : instanceMethodAdvisorConfigs) {
            if (each.isOverrideArgs()) {
                isArgsOverride = true;
            }
            if (null != each.getAdviceClassName()) {
                instanceMethodAroundAdvices.add(loadAdviceInstance(each.getAdviceClassName()));
            }
        }
        return isArgsOverride
                ? new MethodAdvisor<>(methodPointcut, new ComposedInstanceMethodInterceptorArgsOverride(instanceMethodAroundAdvices))
                : new MethodAdvisor<>(methodPointcut, new ComposedInstanceMethodAroundInterceptor(instanceMethodAroundAdvices));
    }
    
    private <T> T loadAdviceInstance(final String adviceClassName) {
        return AdviceInstanceLoader.loadAdviceInstance(adviceClassName, classLoader, pluginConfigs, isEnhancedForProxy);
    }
}