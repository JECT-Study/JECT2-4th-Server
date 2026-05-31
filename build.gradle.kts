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

sourceSets {
	create("unitTest") {
		java.setSrcDirs(listOf("src/unitTest/java"))
		resources.setSrcDirs(listOf("src/unitTest/resources", "src/testFixtures/resources"))
		compileClasspath += sourceSets.main.get().output
		runtimeClasspath += output + compileClasspath
	}

	create("integrationTest") {
		java.setSrcDirs(listOf("src/integrationTest/java"))
		resources.setSrcDirs(listOf("src/integrationTest/resources", "src/testFixtures/resources"))
		compileClasspath += sourceSets.main.get().output
		runtimeClasspath += output + compileClasspath
	}
}

configurations {
	named("unitTestImplementation") {
		extendsFrom(configurations.testImplementation.get())
	}
	named("unitTestRuntimeOnly") {
		extendsFrom(configurations.testRuntimeOnly.get())
	}
	named("integrationTestImplementation") {
		extendsFrom(configurations.testImplementation.get())
	}
	named("integrationTestRuntimeOnly") {
		extendsFrom(configurations.testRuntimeOnly.get())
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
	implementation(Dependencies.SpringBoot.OAUTH2_CLIENT)
	implementation(Dependencies.SpringBoot.WEBSOCKET)

	// Jwt
	implementation(Dependencies.Jwt.API)
	runtimeOnly(Dependencies.Jwt.IMPL)
	runtimeOnly(Dependencies.Jwt.JACKSON)

	// Database
	runtimeOnly(Dependencies.Database.POSTGRESQL)
	runtimeOnly(Dependencies.Database.H2)
	implementation(Dependencies.Database.FLYWAY)
	runtimeOnly(Dependencies.Database.FLYWAY_POSTGRESQL)

	// Swagger / OpenAPI
	implementation(Dependencies.Swagger.SPRINGDOC)

	// Firebase (FCM)
	implementation(Dependencies.Firebase.ADMIN)

	// AWS S3
	implementation(Dependencies.Aws.S3)

	// Google Gemini AI
	implementation(Dependencies.Ai.GEMINI)

	compileOnly(Dependencies.Lombok.LOMBOK)
	annotationProcessor(Dependencies.Lombok.LOMBOK)
	testImplementation(Dependencies.SpringBoot.TEST)
	testImplementation(Dependencies.SpringSecurity.TEST)
	testRuntimeOnly(Dependencies.Test.JUNIT_LAUNCHER)

	// Testcontainers (for Postgres-specific integration tests)
	"integrationTestImplementation"("org.testcontainers:testcontainers:1.21.3")
	"integrationTestImplementation"("org.testcontainers:junit-jupiter:1.21.3")
	"integrationTestImplementation"("org.testcontainers:postgresql:1.21.3")

	// Spring Boot official Testcontainers support
	"integrationTestImplementation"("org.springframework.boot:spring-boot-testcontainers:3.5.11")
}

tasks.withType<Test> {
	useJUnitPlatform()
	outputs.upToDateWhen { false }
	outputs.cacheIf { false }
}

tasks.test {
	enabled = false
}

val unitTest by tasks.registering(Test::class) {
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	description = "Runs unit tests."
	testClassesDirs = sourceSets["unitTest"].output.classesDirs
	classpath = sourceSets["unitTest"].runtimeClasspath
}

val integrationTest by tasks.registering(Test::class) {
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	description = "Runs integration tests. (Testcontainers 기반 실제 PostgreSQL 사용)"
	testClassesDirs = sourceSets["integrationTest"].output.classesDirs
	classpath = sourceSets["integrationTest"].runtimeClasspath
	shouldRunAfter(unitTest)

	// Docker가 없는 환경에서 통합 테스트를 건너뛰기 위한 옵션
	onlyIf {
		!project.hasProperty("skipIntegrationTests")
	}
}

tasks.check {
	setDependsOn(listOf(unitTest, integrationTest))
}

// API Client Generation Tasks
val extractSwagger by tasks.registering(apiclient.ExtractSwaggerTask::class)

val generateApiClient by tasks.registering(apiclient.GenerateApiClientTask::class) {
	dependsOn(extractSwagger)
}

val publishApiClient by tasks.registering(apiclient.PublishApiClientTask::class) {
	dependsOn(generateApiClient)
}
