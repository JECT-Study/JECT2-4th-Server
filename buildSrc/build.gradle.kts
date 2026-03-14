plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.14.0")
    implementation("org.openapitools:openapi-generator:7.14.0")
}
