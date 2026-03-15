package apiclient

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class PublishApiClientTask : DefaultTask() {

    @get:InputDirectory
    val clientDir: File = project.file("generated-api-client")

    init {
        group = "api-client"
        description = "Install, build, and publish the generated API client to npm"
    }

    @TaskAction
    fun publish() {
        runNpm("install")
        runNpm("run", "build")
        runNpm("publish", "--access", "public")

        val version = project.version.toString().removeSuffix("-SNAPSHOT")
        logger.lifecycle("Published @ject-4-vs-team/api-client@$version")
    }

    private fun runNpm(vararg args: String) {
        val result = ProcessBuilder("npm", *args)
            .directory(clientDir)
            .redirectErrorStream(true)
            .start()

        result.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        val exitCode = result.waitFor()
        if (exitCode != 0) throw RuntimeException("npm ${args.first()} failed with exit code $exitCode")
    }
}
