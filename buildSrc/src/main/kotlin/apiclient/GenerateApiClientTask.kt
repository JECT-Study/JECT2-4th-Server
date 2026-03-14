package apiclient

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.openapitools.codegen.DefaultGenerator
import org.openapitools.codegen.config.CodegenConfigurator
import java.io.File

abstract class GenerateApiClientTask : DefaultTask() {

    @get:InputFile
    val swaggerFile: File = project.file("swagger.json")

    @get:InputDirectory
    val templatesDir: File = project.file("openapi-generator")

    @get:OutputDirectory
    val outputDir: File = project.file("generated-api-client")

    init {
        group = "api-client"
        description = "Generate TypeScript Axios API client from swagger.json"
    }

    @TaskAction
    fun generate() {
        if (outputDir.exists()) outputDir.deleteRecursively()

        val config = CodegenConfigurator().apply {
            setInputSpec(swaggerFile.absolutePath)
            setGeneratorName("typescript-axios")
            setOutputDir(outputDir.absolutePath)
            setTemplateDir(templatesDir.absolutePath)
            addAdditionalProperty("supportsES6", "true")
            addAdditionalProperty("withSeparateModelsAndApi", "true")
            addAdditionalProperty("apiPackage", "api")
            addAdditionalProperty("modelPackage", "models")
        }
        DefaultGenerator().opts(config.toClientOptInput()).generate()

        val version = project.version.toString().removeSuffix("-SNAPSHOT")

        // Copy template files with version substitution
        File(templatesDir, "package.json").readText()
            .replace("\"version\": \"0.0.0\"", "\"version\": \"$version\"")
            .let { File(outputDir, "package.json").writeText(it) }

        File(templatesDir, "tsconfig.json").copyTo(File(outputDir, "tsconfig.json"), overwrite = true)

        logger.lifecycle("API client generated at ${outputDir.absolutePath} (version: $version)")
    }
}
