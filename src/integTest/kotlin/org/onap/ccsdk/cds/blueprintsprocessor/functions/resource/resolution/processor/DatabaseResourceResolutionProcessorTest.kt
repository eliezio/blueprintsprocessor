/*
 * Copyright © 2019 IBM, Bell Canada.
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
package org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.processor

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.onap.ccsdk.cds.blueprintsprocessor.core.BluePrintCoreConfiguration
import org.onap.ccsdk.cds.blueprintsprocessor.core.BluePrintProperties
import org.onap.ccsdk.cds.blueprintsprocessor.core.BlueprintProcessorProperties
import org.onap.ccsdk.cds.blueprintsprocessor.core.BlueprintPropertyConfiguration
import org.onap.ccsdk.cds.blueprintsprocessor.db.BluePrintDBLibConfiguration
import org.onap.ccsdk.cds.blueprintsprocessor.db.primary.BluePrintDBLibPropertySevice
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.ResourceAssignmentRuntimeService
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.mock.MockBlueprintProcessorCatalogServiceImpl
import org.onap.ccsdk.cds.blueprintsprocessor.db.mock.MockDatabaseConfiguration
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.utils.ResourceAssignmentUtils
import org.onap.ccsdk.cds.controllerblueprints.core.data.PropertyDefinition
import org.onap.ccsdk.cds.controllerblueprints.core.utils.BluePrintMetadataUtils
import org.onap.ccsdk.cds.controllerblueprints.resource.dict.ResourceAssignment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertNotNull

@RunWith(SpringRunner::class)
@WebFluxTest
@ContextConfiguration(classes = [DatabaseResourceAssignmentProcessor::class, BlueprintPropertyConfiguration::class,
    BluePrintProperties::class, BluePrintDBLibPropertySevice::class, BluePrintDBLibConfiguration::class,
    BluePrintCoreConfiguration::class, MockDatabaseConfiguration::class, MockBlueprintProcessorCatalogServiceImpl::class,
    BlueprintProcessorProperties::class])
@ComponentScan(basePackages = ["org.onap.ccsdk.cds.controllerblueprints"])
@TestPropertySource(locations = ["classpath:application-test.properties"])
class DatabaseResourceResolutionProcessorTest {

    @Autowired
    lateinit var databaseResourceAssignmentProcessor: DatabaseResourceAssignmentProcessor

    @Test
    fun `test database resource resolution processor db`() {
        runBlocking {
            val bluePrintContext = BluePrintMetadataUtils.getBluePrintContext(
                    "./src/test/resources/test-blueprint/baseconfiguration")

            val resourceAssignmentRuntimeService = ResourceAssignmentRuntimeService("1234", bluePrintContext)

            databaseResourceAssignmentProcessor.raRuntimeService = resourceAssignmentRuntimeService
            databaseResourceAssignmentProcessor.resourceDictionaries = ResourceAssignmentUtils
                    .resourceDefinitions(bluePrintContext.rootPath)

            val resourceAssignment = ResourceAssignment().apply {
                name = "service-instance-id"
                dictionaryName = "service-instance-id"
                dictionarySource = "processor-db"
                property = PropertyDefinition().apply {
                    type = "string"
                }
            }

            val processorName = databaseResourceAssignmentProcessor.applyNB(resourceAssignment)
            assertNotNull(processorName, "couldn't get Database resource assignment processor name")
            println(processorName)
        }
    }

    @Test
    fun `test database resource resolution primary db`() {
        runBlocking {
            val bluePrintContext = BluePrintMetadataUtils.getBluePrintContext(
                    "./src/test/resources/test-blueprint/capability_python")

            val resourceAssignmentRuntimeService = ResourceAssignmentRuntimeService("1234", bluePrintContext)

            databaseResourceAssignmentProcessor.raRuntimeService = resourceAssignmentRuntimeService
            databaseResourceAssignmentProcessor.resourceDictionaries = ResourceAssignmentUtils
                    .resourceDefinitions(bluePrintContext.rootPath)

            val resourceAssignment = ResourceAssignment().apply {
                name = "service-instance-id"
                dictionaryName = "service-instance-id"
                dictionarySource = "primary-db"
                property = PropertyDefinition().apply {
                    type = "string"
                }
            }

            val processorName = databaseResourceAssignmentProcessor.applyNB(resourceAssignment)
            assertNotNull(processorName, "couldn't get Database resource assignment processor name")
            println(processorName)
        }
    }
}