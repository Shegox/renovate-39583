plugins {
    alias(libs.plugins.jvm)

    application

    alias(libs.plugins.serialization)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get(), configurations.testImplementation.get())
}
val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get(), configurations.testRuntimeOnly.get())
}

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}

tasks.check { dependsOn(integrationTest) }

dependencies {
    implementation(project(":common"))
    testImplementation(testFixtures(project(":common")))

    // AWS
    implementation(platform(libs.aws.bom))
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:s3-transfer-manager")
    implementation("software.amazon.awssdk:eventbridge")

    // HTTP
    implementation(platform("io.ktor:ktor-bom:3.3.2"))
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-call-logging")

    // Logging
    implementation(platform(libs.slf4j.bom))
    implementation(libs.logback.classic)
    implementation("org.slf4j:jcl-over-slf4j")
    implementation("org.slf4j:jul-to-slf4j")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")

    // JSON
    implementation(libs.json)

    // Open Telemetry
    implementation(platform(libs.opentelemetry.bom))
    implementation("io.opentelemetry:opentelemetry-api")
    testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")
    testRuntimeOnly("io.opentelemetry:opentelemetry-exporter-logging")

    testImplementation("io.opentelemetry:opentelemetry-sdk-trace")
    testImplementation("io.opentelemetry:opentelemetry-exporter-logging")

    // Config
    implementation("com.typesafe:config:1.4.5")

    implementation("io.kubernetes:client-java:23.0.0")
    implementation("io.kubernetes:client-java-extended:23.0.0")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.14.6")

    testImplementation("io.ktor:ktor-client-core")
    testImplementation("io.ktor:ktor-client-cio")
    testImplementation("io.ktor:ktor-client-auth")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    implementation(platform("io.netty:netty-bom:4.2.7.Final")) {
        because("CVE-2025-55163")
    }

    constraints {
        implementation("org.apache.commons", "commons-lang3", "3.20.0") {
            because("See https://jira.concur.com/browse/QEP-7187")
        }
    }
}

configurations.all {
    exclude("commons-logging", "commons-logging")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

application {
    mainClass = "com.sap.concur.testframework.scheduler.MainKt"
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
        lockMode = if(providers.environmentVariable("CI").isPresent) LockMode.STRICT else LockMode.DEFAULT
    }
}
