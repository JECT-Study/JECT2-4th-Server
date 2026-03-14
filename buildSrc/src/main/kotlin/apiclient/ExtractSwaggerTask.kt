package apiclient

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

abstract class ExtractSwaggerTask : DefaultTask() {

    @get:OutputFile
    val swaggerFile: File = project.file("swagger.json")

    private val serverUrl = "http://localhost:8080"
    private val apiDocsPath = "/v3/api-docs"

    init {
        group = "api-client"
        description = "Build boot jar, start server, extract swagger.json, then stop"
        dependsOn("bootJar")
    }

    @TaskAction
    fun extract() {
        val jarFile = project.file("build/libs").listFiles()
            ?.firstOrNull { it.name.endsWith(".jar") && !it.name.contains("plain") }
            ?: throw RuntimeException("Boot jar not found. Run bootJar first.")

        val server = ProcessBuilder("java", "-jar", jarFile.absolutePath)
            .directory(project.projectDir)
            .redirectErrorStream(true)
            .start()

        try {
            waitForServer()
            val json = URI("$serverUrl$apiDocsPath").toURL().readText()
            swaggerFile.writeText(json)
            logger.lifecycle("swagger.json extracted (${swaggerFile.length()} bytes)")
        } finally {
            server.destroyForcibly()
            server.waitFor()
        }
    }

    private fun waitForServer() {
        repeat(30) {
            try {
                URI("$serverUrl$apiDocsPath").toURL().readText()
                return
            } catch (_: Exception) {
                Thread.sleep(2000)
            }
        }
        throw RuntimeException("Server did not start within 60 seconds")
    }
}
