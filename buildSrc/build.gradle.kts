plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.1.21"
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.14.0")
    implementation("org.openapitools:openapi-generator:7.14.0")
}
