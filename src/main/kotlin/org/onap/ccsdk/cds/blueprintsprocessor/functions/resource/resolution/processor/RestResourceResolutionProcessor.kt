/*
 *  Copyright © 2018 IBM.
 *  Modifications Copyright © 2017-2019 AT&T, Bell Canada
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.processor

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.MissingNode
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.ResourceResolutionConstants.PREFIX_RESOURCE_RESOLUTION_PROCESSOR
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.RestResourceSource
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.utils.ResourceAssignmentUtils
import org.onap.ccsdk.cds.blueprintsprocessor.rest.service.BluePrintRestLibPropertyService
import org.onap.ccsdk.cds.blueprintsprocessor.rest.service.BlueprintWebClientService
import org.onap.ccsdk.cds.controllerblueprints.core.*
import org.onap.ccsdk.cds.controllerblueprints.core.utils.JacksonUtils
import org.onap.ccsdk.cds.controllerblueprints.resource.dict.ResourceAssignment
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

/**
 * RestResourceResolutionProcessor
 *
 * @author Kapil Singal
 */
@Service("${PREFIX_RESOURCE_RESOLUTION_PROCESSOR}source-rest")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
open class RestResourceResolutionProcessor(private val blueprintRestLibPropertyService: BluePrintRestLibPropertyService)
    : ResourceAssignmentProcessor() {

    private val logger = LoggerFactory.getLogger(RestResourceResolutionProcessor::class.java)

    override fun getName(): String {
        return "${PREFIX_RESOURCE_RESOLUTION_PROCESSOR}source-rest"
    }

    override suspend fun processNB(resourceAssignment: ResourceAssignment) {
        try {
            validate(resourceAssignment)

            // Check if It has Input
            val value = getFromInput(resourceAssignment)
            if (value == null || value is MissingNode) {
                val dName = resourceAssignment.dictionaryName
                val dSource = resourceAssignment.dictionarySource
                val resourceDefinition = resourceDictionaries[dName]
                        ?: throw BluePrintProcessorException("couldn't get resource dictionary definition for $dName")

                val resourceSource = resourceDefinition.sources[dSource]
                        ?: throw BluePrintProcessorException("couldn't get resource definition $dName source($dSource)")

                val resourceSourceProperties =
                        checkNotNull(resourceSource.properties) { "failed to get source properties for $dName " }

                val sourceProperties =
                        JacksonUtils.getInstanceFromMap(resourceSourceProperties, RestResourceSource::class.java)

                val path = nullToEmpty(sourceProperties.path)
                val inputKeyMapping =
                    checkNotNull(sourceProperties.inputKeyMapping) { "failed to get input-key-mappings for $dName under $dSource properties" }
                val resolvedInputKeyMapping = resolveInputKeyMappingVariables(inputKeyMapping).toMutableMap()

                // Resolving content Variables
                val payload = resolveFromInputKeyMapping(nullToEmpty(sourceProperties.payload), resolvedInputKeyMapping)
                val urlPath =
                        resolveFromInputKeyMapping(checkNotNull(sourceProperties.urlPath), resolvedInputKeyMapping)
                val verb = resolveFromInputKeyMapping(nullToEmpty(sourceProperties.verb), resolvedInputKeyMapping)

                logger.info("$dSource dictionary information : ($urlPath), ($inputKeyMapping), (${sourceProperties.outputKeyMapping})")
                // Get the Rest Client Service
                val restClientService = blueprintWebClientService(resourceAssignment, sourceProperties)

                val response = restClientService.exchangeResource(verb, urlPath, payload)
                val responseStatusCode = response.status
                val responseBody = response.body
                if (responseStatusCode in 200..299 && !responseBody.isBlank()) {
                    populateResource(resourceAssignment, sourceProperties, responseBody, path)
                } else {
                    val errMsg = "Failed to get $dSource result for dictionary name ($dName) using urlPath ($urlPath) response_code: ($responseStatusCode)"
                    logger.warn(errMsg)
                    throw BluePrintProcessorException(errMsg)
                }
            }
            // Check the value has populated for mandatory case
            ResourceAssignmentUtils.assertTemplateKeyValueNotNull(resourceAssignment)
        } catch (e: Exception) {
            ResourceAssignmentUtils.setFailedResourceDataValue(resourceAssignment, e.message)
            throw BluePrintProcessorException("Failed in template key ($resourceAssignment) assignments with: ${e.message}",
                    e)
        }
    }

    fun blueprintWebClientService(resourceAssignment: ResourceAssignment,
                                          restResourceSource: RestResourceSource): BlueprintWebClientService {
        return if (isNotEmpty(restResourceSource.endpointSelector)) {
            val restPropertiesJson = raRuntimeService.resolveDSLExpression(restResourceSource.endpointSelector!!)
            blueprintRestLibPropertyService.blueprintWebClientService(restPropertiesJson)
        } else {
            blueprintRestLibPropertyService.blueprintWebClientService(resourceAssignment.dictionarySource!!)
        }
    }

    @Throws(BluePrintProcessorException::class)
    private fun populateResource(resourceAssignment: ResourceAssignment, sourceProperties: RestResourceSource,
                                 restResponse: String, path: String) {
        val dName = resourceAssignment.dictionaryName
        val dSource = resourceAssignment.dictionarySource
        val type = nullToEmpty(resourceAssignment.property?.type)
        lateinit var entrySchemaType: String

        val outputKeyMapping = checkNotNull(sourceProperties.outputKeyMapping) {
            "failed to get output-key-mappings for $dName under $dSource properties"
        }
        logger.info("Response processing type($type)")

        val responseNode = checkNotNull(JacksonUtils.jsonNode(restResponse).at(path)) {
            "Failed to find path ($path) in response ($restResponse)"
        }
        logger.info("populating value for output mapping ($outputKeyMapping), from json ($responseNode)")


        when (type) {
            in BluePrintTypes.validPrimitiveTypes() -> {
                logger.info("For template key (${resourceAssignment.name}) setting value as ($responseNode)")
                ResourceAssignmentUtils.setResourceDataValue(resourceAssignment, raRuntimeService, responseNode)
            }
            in BluePrintTypes.validCollectionTypes() -> {
                // Array Types
                entrySchemaType = checkNotEmpty(resourceAssignment.property?.entrySchema?.type) {
                    "Entry schema is not defined for dictionary ($dName) info"
                }
                val arrayNode = responseNode as ArrayNode

                if (entrySchemaType !in BluePrintTypes.validPrimitiveTypes()) {

                    val responseArrayNode = responseNode.toList()
                    for (responseSingleJsonNode in responseArrayNode) {

                        val arrayChildNode = JacksonUtils.objectMapper.createObjectNode()

                        outputKeyMapping.map {
                            val responseKeyValue = responseSingleJsonNode.get(it.key)
                            val propertyTypeForDataType = ResourceAssignmentUtils
                                    .getPropertyType(raRuntimeService, entrySchemaType, it.key)

                            logger.info("For List Type Resource: key (${it.key}), value ($responseKeyValue), " +
                                    "type  ({$propertyTypeForDataType})")

                            JacksonUtils.populateJsonNodeValues(it.value,
                                    responseKeyValue, propertyTypeForDataType, arrayChildNode)
                        }
                        arrayNode.add(arrayChildNode)
                    }
                }
                logger.info("For template key (${resourceAssignment.name}) setting value as ($arrayNode)")
                // Set the List of Complex Values
                ResourceAssignmentUtils.setResourceDataValue(resourceAssignment, raRuntimeService, arrayNode)
            }
            else -> {
                // Complex Types
                entrySchemaType = checkNotEmpty(resourceAssignment.property?.type) {
                    "Entry schema is not defined for dictionary ($dName) info"
                }
                val objectNode = JacksonUtils.objectMapper.createObjectNode()
                outputKeyMapping.map {
                    val responseKeyValue = responseNode.get(it.key)
                    val propertyTypeForDataType = ResourceAssignmentUtils
                            .getPropertyType(raRuntimeService, entrySchemaType, it.key)

                    logger.info("For List Type Resource: key (${it.key}), value ($responseKeyValue), type  ({$propertyTypeForDataType})")
                    JacksonUtils.populateJsonNodeValues(it.value, responseKeyValue, propertyTypeForDataType, objectNode)
                }

                logger.info("For template key (${resourceAssignment.name}) setting value as ($objectNode)")
                // Set the List of Complex Values
                ResourceAssignmentUtils.setResourceDataValue(resourceAssignment, raRuntimeService, objectNode)
            }
        }
    }

    @Throws(BluePrintProcessorException::class)
    private fun validate(resourceAssignment: ResourceAssignment) {
        checkNotEmpty(resourceAssignment.name) { "resource assignment template key is not defined" }
        checkNotEmpty(resourceAssignment.dictionaryName) {
            "resource assignment dictionary name is not defined for template key (${resourceAssignment.name})"
        }
        checkNotEmpty(resourceAssignment.dictionarySource) {
            "resource assignment dictionary source is not defined for template key (${resourceAssignment.name})"
        }
    }

    override suspend fun recoverNB(runtimeException: RuntimeException, resourceAssignment: ResourceAssignment) {
        raRuntimeService.getBluePrintError().addError(runtimeException.message!!)
    }

}
