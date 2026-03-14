object Dependencies {

    object SpringBoot {
        const val DATA_JPA = "org.springframework.boot:spring-boot-starter-data-jpa"
        const val SECURITY = "org.springframework.boot:spring-boot-starter-security"
        const val VALIDATION = "org.springframework.boot:spring-boot-starter-validation"
        const val WEB = "org.springframework.boot:spring-boot-starter-web"
        const val TEST = "org.springframework.boot:spring-boot-starter-test"
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

    object Test {
        const val JUNIT_LAUNCHER = "org.junit.platform:junit-platform-launcher"
    }
}
