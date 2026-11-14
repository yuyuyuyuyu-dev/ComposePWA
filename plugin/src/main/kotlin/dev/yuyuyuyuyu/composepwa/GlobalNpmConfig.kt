package dev.yuyuyuyuyu.composepwa

import org.gradle.process.ExecSpec
import java.io.File
import java.util.concurrent.TimeUnit

internal fun keepGlobalNpmConfig(spec: ExecSpec) {
    val npmArgs = spec.args
    val npmCommandIndex = npmArgs.indexOf(NPM_COMMAND)
    val npmLauncher = listOf(spec.executable) + npmArgs.take(npmCommandIndex)
    val environment = spec.environment.mapValues { it.value.toString() }
    val globalConfig =
        runNpm(npmLauncher + listOf("exec", "--global", "--call", PRINT_GLOBAL_CONFIG), spec.workingDir, environment)

    val (beforeOptions, afterOptions) = npmArgs.withIndex().partition { it.index <= npmCommandIndex }
    spec.setArgs(
        beforeOptions.map { it.value } + listOf("--globalconfig", globalConfig) + afterOptions.map { it.value },
    )
}

private fun runNpm(
    command: List<String>,
    workingDir: File,
    environment: Map<String, String>,
): String {
    val errorFile = File.createTempFile("composepwa-npm", ".log").apply { deleteOnExit() }
    val processBuilder =
        ProcessBuilder(command)
            .directory(workingDir)
            .redirectError(errorFile)
    processBuilder.environment().apply {
        clear()
        putAll(environment)
    }
    val process = processBuilder.start()
    val output = process.inputStream.bufferedReader().readText()
    val finished = process.waitFor(NPM_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    check(finished && process.exitValue() == 0) {
        "`${command.joinToString(" ")}` failed:\n${errorFile.readText()}"
    }
    return output.trim()
}

private const val NPM_COMMAND = "exec"
private const val NPM_TIMEOUT_SECONDS = 60L
private const val PRINT_GLOBAL_CONFIG =
    "node -p \"process.env.npm_config_globalconfig" +
        "||require('path').join(process.env.npm_config_global_prefix,'etc','npmrc')\""
