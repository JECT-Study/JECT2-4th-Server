object Dependencies {

    object SpringBoot {
        const val ACTUATOR = "org.springframework.boot:spring-boot-starter-actuator"
        const val DATA_JPA = "org.springframework.boot:spring-boot-starter-data-jpa"
        const val SECURITY = "org.springframework.boot:spring-boot-starter-security"
        const val VALIDATION = "org.springframework.boot:spring-boot-starter-validation"
        const val WEB = "org.springframework.boot:spring-boot-starter-web"
        const val TEST = "org.springframework.boot:spring-boot-starter-test"
        const val OAUTH2_CLIENT = "org.springframework.boot:spring-boot-starter-oauth2-client"
        const val WEBSOCKET = "org.springframework.boot:spring-boot-starter-websocket"
    }

    object Jwt {
        const val API = "io.jsonwebtoken:jjwt-api:0.12.6"
        const val IMPL = "io.jsonwebtoken:jjwt-impl:0.12.6"
        const val JACKSON = "io.jsonwebtoken:jjwt-jackson:0.12.6"
    }

    object SpringSecurity {
        const val TEST = "org.springframework.security:spring-security-test"
    }

    object Swagger {
        const val SPRINGDOC = "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6"
    }

    object Lombok {
        const val LOMBOK = "org.projectlombok:lombok"
    }

    object Database {
        const val H2 = "com.h2database:h2"
        const val POSTGRESQL = "org.postgresql:postgresql"
        const val FLYWAY = "org.flywaydb:flyway-core"
        const val FLYWAY_POSTGRESQL = "org.flywaydb:flyway-database-postgresql"
    }

    object Test {
        const val JUNIT_LAUNCHER = "org.junit.platform:junit-platform-launcher"
    }

    object Firebase {
        const val ADMIN = "com.google.firebase:firebase-admin:9.3.0"
    }
}
