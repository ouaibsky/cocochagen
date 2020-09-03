package org.icroco.cococha

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import org.icroco.cococha.git.GitService
import org.icroco.cococha.git.SemanticVersion
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.util.*


class CoCoChaGenApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // assume SLF4J is bound to logback in the current environment
            // assume SLF4J is bound to logback in the current environment
            // print logback's internal status
            // print logback's internal status
//            StatusPrinter.print(LoggerFactory.getILoggerFactory() as LoggerContext)

            val commandLine = CommandLine(CoCoChaCmd())
//            commandLine.isOptionsCaseInsensitive = true
            System.exit(commandLine.execute(*args))
        }
    }
}

class PropertiesVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        val p = Properties()
        p.load(javaClass.getResourceAsStream("/META-INF/build-info.properties"))
        return arrayOf(p.getProperty("build.version", "?!?"))
    }
}

@CommandLine.Command(name = "cococha",
        mixinStandardHelpOptions = true,
        description = ["@|bold Conventional Commit Changelog|@ @|underline Generator|@", ""], versionProvider = PropertiesVersionProvider::class
)
class CoCoChaCmd() : Runnable {

    @CommandLine.Option(names = ["-o", "--output"], description = ["output changelog filename",
        "Default value is '\${DEFAULT-VALUE}'"], defaultValue = "CHANGELOG.md")
    private var outputFile = "CHANGELOG.md"

    @CommandLine.Option(names = ["-c", "--release-count"], defaultValue = "1", description = ["Number of last releases to include into this changelog.",
        " default is '\${DEFAULT-VALUE}'"])
    private var releaseCount = 1

    @CommandLine.Option(names = ["-t", "--commit-type"], defaultValue = "feat,fix", description = ["Filter only those commits type",
        "Default value is '\${DEFAULT-VALUE}'"], split = ",")
    private var commitType: List<String> = listOf("feat,fix")

    @CommandLine.Option(names = ["-l", "--list-tags"], defaultValue = "false", description = ["Filter only those commits type",
        "Default value is '\${DEFAULT-VALUE}'"])
    private var listTag: Boolean = false

    @CommandLine.Option(names = ["-s", "--start-tag"], description = ["Tag to start looking commits"])
    private var startTag: String? = null

    @CommandLine.Option(names = ["-v", "--verbose"], required = false, description = ["print more information on console",
        "Default value is '\${DEFAULT-VALUE}'"])
    private var verbose: Boolean = false

    override fun run() {
        if (verbose) {
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
            val l = org.slf4j.LoggerFactory.getLogger(javaClass.packageName) as ch.qos.logback.classic.Logger
            l.level = Level.DEBUG
        }
        val gitService = GitService()

        val tags = gitService.getSemanticVersionTag()
        if (listTag) {
            tags.forEach { println(it) }
        }
        val semVer = if (startTag == null) tags.first() else (SemanticVersion(startTag!!))
        println("StartTag: $semVer")
    }
}
//
//fun main(args: Array<String>) {
//    val commandLine = CommandLine(CoCoChaCmd())
//    commandLine.isOptionsCaseInsensitive = true
//    System.exit(commandLine.execute(*args))
//}