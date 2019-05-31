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

import com.fasterxml.jackson.databind.node.TextNode
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.ResourceAssignmentRuntimeService
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.utils.ResourceAssignmentUtils
import org.onap.ccsdk.cds.controllerblueprints.core.data.PropertyDefinition
import org.onap.ccsdk.cds.controllerblueprints.core.utils.BluePrintMetadataUtils
import org.onap.ccsdk.cds.controllerblueprints.resource.dict.ResourceAssignment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [InputResourceResolutionProcessor::class])
@TestPropertySource(locations = ["classpath:application-test.properties"])
class InputResourceResolutionProcessorTest {

    @Autowired
    lateinit var inputResourceResolutionProcessor: InputResourceResolutionProcessor

    @Test
    fun `InputResourceResolutionProcessor should be able to resolve a value for an input parameter`() {
        runBlocking {
            val bluePrintContext = BluePrintMetadataUtils.getBluePrintContext(
                    "./src/test/resources/test-blueprint/baseconfiguration")

            val resourceAssignmentRuntimeService = Mockito.spy(ResourceAssignmentRuntimeService("1234", bluePrintContext))

            inputResourceResolutionProcessor.raRuntimeService = resourceAssignmentRuntimeService
            inputResourceResolutionProcessor.resourceDictionaries = ResourceAssignmentUtils.resourceDefinitions(bluePrintContext.rootPath)

            // mocking input
            val textNode = TextNode("any value")
            Mockito.doReturn(textNode).`when`(resourceAssignmentRuntimeService).getInputValue(any())

            val resourceAssignment = ResourceAssignment().apply {
                name = "rr-name"
                dictionaryName = "hostname"
                dictionarySource = "input"
                property = PropertyDefinition().apply {
                    type = "string"
                }
            }

            val processorName = inputResourceResolutionProcessor.applyNB(resourceAssignment)
            assertTrue(processorName, "An error occurred while trying to test the InputResourceResolutionProcessor")
        }
    }

    // Need to wrap the Mockito.any, because otherwise it will throw the exception
    //      java.lang.IllegalStateException: Mockito.any() must not be nul
    // This happens because Kotlin checks for nullability and Mockito.any() returns null in their implementations. Kotlin
    // detects the potential null pointer and throws an exception. By wrapping with a custom any() that returns null as T,
    // this issue is avoided.
    private fun <T> any(): T {
        Mockito.any<T>()
        return null as T
    }
}
