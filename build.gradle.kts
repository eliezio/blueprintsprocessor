/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.3.21")
    groovy
    jacoco

    id("nebula.project").version("6.0.3")
    id("nebula.integtest").version("6.0.3")

    id("org.springframework.boot").version("2.1.5.RELEASE")

    id("nebula.release").version("9.2.0")

    id("info.solidsoft.pitest").version("1.4.0")

    // Quality / Documentation Plugins
    id("org.sonarqube").version("2.7.1")
    id("com.adarshr.test-logger").version("1.7.0")
    id("com.github.ksoichiro.console.reporter").version("0.6.2")

    id("com.google.cloud.tools.jib").version("1.2.0")
}

apply(plugin = "io.spring.dependency-management")

description = "Blueprints Processor"
group = "org.onap.ccsdk.cds.blueprintsprocessor"
val defaultDockerGroup = "onap"
val dockerGroup: String? by project
val scmGroup = "excelsior-esy"
val scmProject = "blueprintsprocessor"

val mainClassName = "org.onap.ccsdk.cds.blueprintsprocessor.BlueprintProcessorApplication"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

configurations {
    compile {
        exclude(module = "slf4j-simple")
    }
    integTestImplementation {
        // Conflicts with "com.vaadin.external.google:android-json".
        // See https://github.com/spring-projects/spring-boot/issues/8706
        exclude(group = "org.json", module = "json")
    }
}

val ccsdkVersion = "0.5.0-SNAPSHOT"

val mockkVersion = "1.9"
val mockServerVersion = "5.5.1"
val grpcVersion = "1.18.0"
val sshdVersion = "2.2.0"
val jythonVersion = "2.7.1"
val dmaapClientVersion = "1.1.5"

val spockFrameworkVersion = "1.3-groovy-2.5"
val spockReportsVersion = "1.6.2"

val onapSnapshotsRepo: String by project
val onapReleasesRepo: String by project
val onapPublicRepo: String by project

repositories {
    jcenter()
    maven { url = uri(onapReleasesRepo) }
    maven { url = uri(onapPublicRepo) }
    maven { url = uri(onapSnapshotsRepo) }
}

dependencies {
    implementation(kotlin("stdlib"))

    // Other CDS sibling projects
    implementation("org.onap.ccsdk.cds.components", "proto-definition", ccsdkVersion)
    implementation("org.onap.ccsdk.cds.controllerblueprints", "blueprint-core", ccsdkVersion)
    implementation("org.onap.ccsdk.cds.controllerblueprints", "blueprint-validation", ccsdkVersion)
    implementation("org.onap.ccsdk.cds.controllerblueprints", "db-resources", ccsdkVersion)
    implementation("org.onap.ccsdk.cds.controllerblueprints", "blueprint-scripts", ccsdkVersion)

    // Other CCSDK projects
    implementation("org.onap.ccsdk.sli.core", "sli-common", ccsdkVersion)
    implementation("org.onap.ccsdk.sli.core", "sli-provider-base", ccsdkVersion)

    // ONAP projects
    implementation("org.onap.dmaap.messagerouter.dmaapclient", "dmaapClient", dmaapClientVersion)

    implementation("org.python", "jython-standalone", jythonVersion)

    implementation("org.springframework.boot", "spring-boot-starter")
    implementation("org.springframework.boot", "spring-boot-starter-webflux")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.boot", "spring-boot-starter-security")

    implementation("org.springframework.kafka", "spring-kafka")

    implementation("org.apache.httpcomponents", "httpclient")
    implementation("org.apache.sshd", "sshd-core", sshdVersion)

    runtime("org.mariadb.jdbc", "mariadb-java-client")

    testImplementation("io.mockk", "mockk", mockkVersion)
    testImplementation("org.spockframework", "spock-core", spockFrameworkVersion)
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    testRuntime("com.athaydes", "spock-reports", spockReportsVersion)
    testRuntime("org.glassfish.jersey.inject", "jersey-hk2", "2.27")

    integTestImplementation("org.mock-server", "mockserver-netty", mockServerVersion)
    integTestImplementation("io.grpc", "grpc-testing", grpcVersion)
    integTestImplementation("org.springframework.boot", "spring-boot-starter-test")
    integTestImplementation("org.apache.tomcat.embed", "tomcat-embed-core")
    integTestImplementation("org.spockframework", "spock-spring", spockFrameworkVersion)
    integTestImplementation("ch.qos.logback", "logback-classic")

    integTestRuntime("com.h2database", "h2")
}

val publicReportsDir = "public"
val spockReportsDir = "$publicReportsDir/spock"
val jacocoHtmlReportsDir = "$publicReportsDir/jacoco"
val pitestReportsDir = "$publicReportsDir/pitest"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

/*
 * All Archives
 */
tasks.withType<AbstractArchiveTask> {
    //** reproducible build
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<Test> {
    // JVM flags to speed-up test executions
    jvmArgs("-noverify", "-XX:TieredStopAtLevel=1")
}

/*
 * Reproducible Build
 */
tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

infoBroker {
    excludedManifestProperties = listOf("Build-Date", "Built-OS", "Built-By", "Build-Host")
}

fun File.removeLines(predicate: (String) -> Boolean) {
    val lines = readLines().filterNot(predicate)
    printWriter().use { out ->
        lines.forEach { out.println(it) }
    }
}

springBoot {
    // to create META-INF/build-info.properties. Its contents are exported by /info
    buildInfo {
        properties {
            time = null
        }

        // Watch https://github.com/spring-projects/spring-boot/issues/14494
        doLast {
            File(destinationDir, "build-info.properties")
                    .removeLines { it.startsWith("#") }
        }
    }
}

/*
 * Unit Tests
 */
tasks.test {
    jvmArgs("-noverify")
}

/*
 * Integration Tests
 */
tasks.integrationTest {
    // faster start-up time
    jvmArgs("-noverify", "-XX:TieredStopAtLevel=2")
    systemProperty("com.athaydes.spockframework.report.outputDir", spockReportsDir)
}

/*
 * JaCoCo
 */
// Why we can't use just "task.jacocoTestReport": https://github.com/gradle/kotlin-dsl/issues/1176#issuecomment-435816812
tasks.getByName<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>().asIterable())

    executionData(file("$buildDir/jacoco/test.exec"), file("$buildDir/jacoco/integrationTest.exec"))

    reports {
        html.isEnabled = true
        html.destination = file(jacocoHtmlReportsDir)
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it) {
                exclude("**/${mainClassName.substringAfterLast('.')}.class")
            }
        }))
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    executionData(tasks.jacocoTestReport.get().executionData)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.reportCoverage {
    dependsOn(tasks.jacocoTestReport)
}

/*
 * Pitest
 */
pitest {
    pitestVersion = "1.4.8"
    testSourceSets = setOf(sourceSets.test.get(), sourceSets["integTest"])
    excludedClasses = setOf(mainClassName)

    //** reproducible build
    timestampedReports = false
}

tasks.pitest {
    reportDir = file(pitestReportsDir)
}

/*
 * SonarQube
 */
sonarqube {
    properties {
        properties(mapOf(
                "sonar.projectKey" to "${scmGroup}_$scmProject",
                "sonar.organization" to scmGroup,
                "sonar.links.homepage" to "https://github.com/$scmGroup/$scmProject",
                "sonar.links.ci" to "https://travis-ci.org/$scmGroup/$scmProject",
                "sonar.links.scm" to "https://github.com/$scmGroup/$scmProject",
                "sonar.links.issue" to "https://github.com/$scmGroup/$scmProject/issues"
        ))
    }
}

/*
 * Docker Image
 */
val dockerRegistry: String? by project

jib {
    from {
        // Smaller than the default gcr/distroless/java
        image = "openjdk:8-jre-alpine"
    }
    to {
        val tagVersion = version.toString().substringBefore('-')
        image = listOfNotNull(dockerRegistry, dockerGroup ?: defaultDockerGroup, "${project.name}:$tagVersion")
                .joinToString("/")
    }
    extraDirectories {
        permissions = mapOf("/usr/local/bin/wait-for" to "755")
    }
    container {
        jvmFlags = listOf(
                "-noverify", "-XX:TieredStopAtLevel=2",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseCGroupMemoryLimitForHeap",
                // See http://www.thezonemanager.com/2015/07/whats-so-special-about-devurandom.html
                "-Djava.security.egd=file:/dev/./urandom"
        )
        ports = listOf("8080")
        volumes = listOf("/data")
    }
}

tasks.jibDockerBuild {
    dependsOn(tasks.build)
}

/*
 * Clean-up
 */
tasks.register<Delete>("deletePyClasses") {
    delete(fileTree("src/test/resources") {
        include("**/*\$py.class")
    })
}

tasks.clean {
    dependsOn("deletePyClasses")
}
