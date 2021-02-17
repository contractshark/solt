package com.github.hjubb.solt

import io.ktor.client.features.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.default
import pw.binom.Environment
import pw.binom.getEnv

val nodeRegex =
    Regex("import\\s+?(?:(?:(?:[\\w*\\s{},]*)\\s+from\\s+?)|)(?:[\"|'](?!\\.{0,2}\\/)(.*?)[\"|'])[\\s]*?(?:;|\$|)")
val relativeRegex =
    Regex("import\\s+?(?:(?:(?:[\\w*\\s{},]*)\\s+from\\s+?)|)(?:[\"|'](\\.{0,2}\\/.*?)[\"|'])[\\s]*?(?:;|\$|)")

@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser("solt")

    val version by parser.option(ArgType.Boolean, shortName = "v", description = "print solt version").default(false)

    parser.subcommands(Writer(), Verifier())

    try {
        parser.parse(args)
        if (version) {
            println("solt version: ${BuildKonfig.version}")
        }
    } catch (e: Exception) {
        if (Environment.getEnv("DEBUG") != null) {
            e.printStackTrace()
        } else {
            println("${e::class.simpleName} ${e.message}")
        }
    }
}