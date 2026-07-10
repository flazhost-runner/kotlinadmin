import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("jvm")                        version "2.1.0"
    kotlin("plugin.serialization")       version "2.1.0"
    id("io.ktor.plugin")                 version "3.1.3"
    id("io.gitlab.arturbosch.detekt")    version "1.23.7"
    application
}

group   = "com.kotlinadmin"
version = "0.0.1"

application {
    mainClass.set("com.kotlinadmin.ApplicationKt")
}

// ── version pins ────────────────────────────────────────────────────────────
val ktorVersion     = "3.1.3"
val exposedVersion  = "0.55.0"
val flywayVersion   = "10.17.3"
val koinVersion     = "4.0.0"
val lettuceVersion  = "6.4.0.RELEASE"
val kotestVersion   = "5.9.1"
val cucumberVersion = "7.20.1"

// ── JVM toolchain ───────────────────────────────────────────────────────────
kotlin {
    jvmToolchain(21)
}

// ── dependencies ─────────────────────────────────────────────────────────────
dependencies {

    // Ktor — server core + engine
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    // Ktor — templating & content
    implementation("io.ktor:ktor-server-freemarker:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Ktor — sessions, auth, rate-limit, status-pages, CORS, compression
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-rate-limit:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Database driver + migrations
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    // DI — Koin
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Security
    implementation("org.mindrot:jbcrypt:0.4")

    // HTML sanitizer
    implementation("org.jsoup:jsoup:1.17.2")

    // Redis — lettuce (session store + JWT blacklist)
    implementation("io.lettuce:lettuce-core:$lettuceVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.8")

    // ── test ────────────────────────────────────────────────────────────────
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    // Cucumber BDD
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.3")

    // Koin test utilities
    testImplementation("io.insert-koin:koin-test:$koinVersion")

    // Detekt — analysis only, plugin already applied above
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}

// ── test ─────────────────────────────────────────────────────────────────────
tasks.withType<Test> {
    useJUnitPlatform()
}

// ── detekt ───────────────────────────────────────────────────────────────────
detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}
