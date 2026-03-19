plugins {
	java
	id("org.springframework.boot") version "3.5.11"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ject"
version = "0.0.1-SNAPSHOT"
description = "Ject2-4th-server"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(Dependencies.SpringBoot.DATA_JPA)
	implementation(Dependencies.SpringBoot.SECURITY)
	implementation(Dependencies.SpringBoot.VALIDATION)
	implementation(Dependencies.SpringBoot.WEB)
	implementation(Dependencies.SpringBoot.ACTUATOR)

	// Database
	runtimeOnly(Dependencies.Database.H2)

	// Swagger / OpenAPI
	implementation(Dependencies.Swagger.SPRINGDOC)

	compileOnly(Dependencies.Lombok.LOMBOK)
	annotationProcessor(Dependencies.Lombok.LOMBOK)
	testImplementation(Dependencies.SpringBoot.TEST)
	testImplementation(Dependencies.SpringSecurity.TEST)
	testRuntimeOnly(Dependencies.Test.JUNIT_LAUNCHER)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// API Client Generation Tasks
val extractSwagger by tasks.registering(apiclient.ExtractSwaggerTask::class)

val generateApiClient by tasks.registering(apiclient.GenerateApiClientTask::class) {
	dependsOn(extractSwagger)
}

val publishApiClient by tasks.registering(apiclient.PublishApiClientTask::class) {
	dependsOn(generateApiClient)
}
