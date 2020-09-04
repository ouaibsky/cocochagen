package org.icroco.cococha

import ch.qos.logback.classic.Level
import picocli.CommandLine
import java.nio.file.Path
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
                     description = ["@|bold Conventional Commit Changelog|@ @|underline Generator|@", ""],
                     versionProvider = PropertiesVersionProvider::class
)
class CoCoChaCmd() : Runnable {

    @CommandLine.Option(names = ["-o", "--output"], description = ["output changelog filename",
        "Default value is '\${DEFAULT-VALUE}'"], defaultValue = "CHANGELOG.md")
    private var outputFile = "CHANGELOG.md"

    @CommandLine.Option(names = ["-c", "--release-count"],
                        defaultValue = "1",
                        description = ["Last N releases to include into this changelog.",
                            "Default is '\${DEFAULT-VALUE}'"])
    private var releaseCount = 1

    @CommandLine.Option(names = ["-t", "--commit-type"],
                        defaultValue = "fix,feat,perf",
                        description = ["Filter only those commits type",
                            "Default value is '\${DEFAULT-VALUE}'"],
                        split = ",")
    private var commitType: List<String> = listOf(CommitType.BUG_FIX.prefix,
                                                  CommitType.FEAT.prefix,
                                                  CommitType.PERFORMANCE.prefix)

//    @CommandLine.Option(names = ["-f", "--from-tag"], description = ["Start changelog generation from this tag"])
//    private var fromTag: String? = null

    @CommandLine.Option(names = ["-n", "--release-name"], description = ["Provide the name of this release",
        "By default is automatically computed if you follow semantic versioning"])
    private var releaseName: String? = null

    @CommandLine.Option(names = ["-T", "--tracker-url"], description = ["Tracker URL (Jira. github ...)",
        "If a card id is found is will be tail at the end"])
    private var trackerUrl: String? = null

    @CommandLine.Option(names = ["-F", "--template-file"], description = ["Template file path",
        "Used to override the default changelog template. We use Mustache engine."])
    private var template: Path? = null

    @CommandLine.Option(names = ["-g", "--git-remote-url"], description = ["Git Remote URL (github, gitlab ...)",
        "Remote url to add commit link"])
    private var gitRemoteUrl: String? = null

    @CommandLine.Option(names = ["-v", "--verbose"],
                        required = false,
                        description = ["print more information on console",
                            "Default value is '\${DEFAULT-VALUE}'"])
    private var verbose: Boolean = false

    override fun run() {
        if (verbose) {
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
            val l = org.slf4j.LoggerFactory.getLogger(javaClass.packageName) as ch.qos.logback.classic.Logger
            l.level = Level.DEBUG
        }

        val params = GeneratorParams(template,
                                     releaseName,
                                     outputFile,
                                     releaseCount,
                                     if (commitType.equals("*")) CommitType.values().toList()
                                     else commitType.map { CommitType.of(it) },
                                     gitRemoteUrl,
                                     trackerUrl)
        ChangelogGenerator(params).run()
    }
}
//
//fun main(args: Array<String>) {
//    val commandLine = CommandLine(CoCoChaCmd())
//    commandLine.isOptionsCaseInsensitive = true
//    System.exit(commandLine.execute(*args))
//}