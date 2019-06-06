/*
 * ============LICENSE_START=======================================================
 * ONAP - CDS
 * ================================================================================
 * Copyright (C) 2019 Huawei Technologies Co., Ltd. All rights reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

package org.ccsdk.cds.blueprintprocessor.dmaap

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.onap.ccsdk.cds.blueprintsprocessor.core.BluePrintProperties
import org.onap.ccsdk.cds.blueprintsprocessor.core.BlueprintPropertyConfiguration
import org.onap.ccsdk.cds.blueprintsprocessor.dmaap.BluePrintDmaapLibConfiguration
import org.onap.ccsdk.cds.blueprintsprocessor.dmaap.BluePrintDmaapLibPropertyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit test cases for DMaap publisher code.
 */
@RunWith(SpringRunner::class)
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = [BluePrintDmaapLibConfiguration::class, TestController::class,
    SecurityConfiguration::class,
    BlueprintPropertyConfiguration::class, BluePrintProperties::class])
@TestPropertySource(properties = ["server.port=9111",
    "blueprintsprocessor.dmaapclient.aai.topic=cds_aai",
    "blueprintsprocessor.dmaapclient.aai.type=HTTPNOAUTH",
    "blueprintsprocessor.dmaapclient.aai.host=127.0.0.1:9111",
    "blueprintsprocessor.dmaapclient.multi.topic=cds_multi1,cds_multi2",
    "blueprintsprocessor.dmaapclient.multi.type=HTTPNOAUTH",
    "blueprintsprocessor.dmaapclient.multi.host=127.0.0.1:9111"])
class TestDmaapEventPublisher {

    @Autowired
    lateinit var dmaapService : BluePrintDmaapLibPropertyService

    @Test
    fun `Tests the event properties being set properly and sent as request`() {
        val dmaapClient = dmaapService.blueprintDmaapClientService("aai")

        val strList = listOf("""{ "a" : "hello" }""")
        dmaapClient.sendMessage(strList)
        val msgs = dmaapClient.close(2)
        assertEquals(1, msgs!!.size)
        val topic1 = msgs[0]
        assertEquals(0, topic1.size)
    }

    @Test
    fun `Tests the event properties being set properly and sent as request with single message`() {
        val dmaapClient = dmaapService.blueprintDmaapClientService("aai")
        val str = """{ "a" : "hello" }"""
        dmaapClient.sendMessage(str)
        val msgs = dmaapClient.close(2)
        assertEquals(1, msgs!!.size)
        val topic1 = msgs[0]
        assertEquals(0, topic1.size)
    }

    @Test
    fun `Tests the event properties with multiple topics`() {
        val dmaapClient = dmaapService.blueprintDmaapClientService("multi")

        val strList = listOf("""{ "a" : "hello" }""")
        dmaapClient.sendMessage(strList)
        val msgs = dmaapClient.close(2)
        assertEquals(2, msgs!!.size)
        val topic1 = msgs[0]
        assertEquals(0, topic1.size)
        val topic2 = msgs[1]
        assertEquals(0, topic2.size)
    }


    @Test
    fun `Tests the event properties with multiple topics with JSON node as input`() {
        val jsonString = """{
    "topic" : "cds_json1,cds_json2",
    "type" : "HTTPNOAUTH",
    "host" : "127.0.0.1:9111"
}"""
        val mapper = ObjectMapper()
        val node = mapper.readTree(jsonString)
        val dmaapClient = dmaapService.blueprintDmaapClientService(node)

        val strList = listOf("""{ "a" : "hello" }""")
        dmaapClient.sendMessage(strList)
        val msgs = dmaapClient.close(2)
        assertEquals(2, msgs!!.size)
        val topic1 = msgs[0]
        assertEquals(0, topic1.size)
        val topic2 = msgs[1]
        assertEquals(0, topic2.size)
    }


    @Test
    fun `Tests the event properties with multiple messages`() {
        val dmaapClient = dmaapService.blueprintDmaapClientService("aai")

        val strList = listOf("""{ "a" : "hello" }""", """{ "a" : "second" }""")
        dmaapClient.sendMessage(strList)
        val msgs = dmaapClient.close(2)
        assertEquals(1, msgs!!.size)
        val topic1 = msgs[0]
        assertEquals(0, topic1.size)
    }

    @Test
    fun `Tests the DMAAP client properties generated with the complete prefix`() {
        val properties = dmaapService.dmaapClientProperties(
            "blueprintsprocessor.dmaapclient.aai")
        assertNotNull(properties, "failed to create property bean")
        assertNotNull(properties.host, "failed to get url property in property bean")
    }

    @Test
    fun `Tests the blueprint DMAAP client service with only selector prefix`() {
        val blueprintDmaapClientService =
            dmaapService.blueprintDmaapClientService("aai")
        assertNotNull(blueprintDmaapClientService,
            "failed to create blueprintDmaapClientService")
    }

}

/**
 * Rest controller for testing the client request that is sent.
 */
@RestController
@RequestMapping(path = ["/events"])
open class TestController {

    /**
     * Accepts request for a topic and sends a message as response.
     */
    @PostMapping(path = ["/{topic}"])
    fun postTopic(@PathVariable(value = "topic") topic : String):
            ResponseEntity<Any> {
        val msg = """{ "message" : "The message is published into $topic topic" }"""
        return ResponseEntity(msg, HttpStatus.OK)
    }
}

/**
 * Required since 'spring-security' is on the classpath
 */
open class SecurityConfiguration {

    @Bean
    open fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
            http.csrf().disable()
                    .authorizeExchange().anyExchange().permitAll()
                    .and().build()
}