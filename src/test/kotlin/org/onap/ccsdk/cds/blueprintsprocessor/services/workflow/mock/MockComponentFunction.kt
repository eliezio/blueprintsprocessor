/*
 * Copyright © 2017-2018 AT&T Intellectual Property.
 * Modifications Copyright © 2019 IBM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onap.ccsdk.cds.blueprintsprocessor.services.workflow.mock

import org.onap.ccsdk.cds.blueprintsprocessor.core.api.data.ExecutionServiceInput
import org.onap.ccsdk.cds.blueprintsprocessor.services.execution.AbstractComponentFunction
import org.onap.ccsdk.cds.controllerblueprints.core.asJsonPrimitive
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Configuration
open class MockComponentConfiguration {

    @Bean(name = ["component-resource-assignment", "component-jython-executor"])
    open fun createComponentFunction(): AbstractComponentFunction {
        return MockComponentFunction()
    }

    @Bean(name = ["component-resource-resolution"])
    @ConditionalOnMissingBean(name = ["component-resource-resolution"])
    open fun createComponentResourceResolutionFunction(): AbstractComponentFunction {
        return MockComponentFunction()
    }
}

class MockComponentFunction : AbstractComponentFunction() {

    private val log = LoggerFactory.getLogger(MockComponentFunction::class.java)

    override suspend fun processNB(executionRequest: ExecutionServiceInput) {
        log.info("Processing component : $operationInputs")

        bluePrintRuntimeService.setNodeTemplateAttributeValue(nodeTemplateName,
                "assignment-params", "params".asJsonPrimitive())
    }

    override suspend fun recoverNB(runtimeException: RuntimeException, executionRequest: ExecutionServiceInput) {
        log.info("Recovering component..")
    }
}

@Component("singleton-function")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class SingletonComponentFunction : AbstractComponentFunction() {

    private val log = LoggerFactory.getLogger(MockComponentFunction::class.java)

    override suspend fun processNB(executionRequest: ExecutionServiceInput) {
        log.info("Processing component : $operationInputs")
    }

    override suspend fun recoverNB(runtimeException: RuntimeException, executionRequest: ExecutionServiceInput) {
        log.info("Recovering component..")
    }
}

@Component("prototype-function")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PrototypeComponentFunction : AbstractComponentFunction() {

    private val log = LoggerFactory.getLogger(MockComponentFunction::class.java)

    override suspend fun processNB(executionRequest: ExecutionServiceInput) {
        log.info("Processing component : $operationInputs")
    }

    override suspend fun recoverNB(runtimeException: RuntimeException, executionRequest: ExecutionServiceInput) {
        log.info("Recovering component..")
    }
}