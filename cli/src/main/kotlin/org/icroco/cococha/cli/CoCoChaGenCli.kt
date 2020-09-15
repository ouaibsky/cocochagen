package org.icroco.cococha.cli

import ch.qos.logback.classic.Level
import org.icroco.cococha.generator.ChangelogGenerator
import org.icroco.cococha.generator.CommitType
import org.icroco.cococha.generator.GeneratorParams
import org.icroco.cococha.generator.defaultIssueRegex
import picocli.CommandLine
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import kotlin.system.exitProcess


class CoCoChaGenCli {
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
            exitProcess(commandLine.execute(*args))
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
                     versionProvider = PropertiesVersionProvider::class,
                     usageHelpWidth = 140,
                     showDefaultValues = false)
class CoCoChaCmd : Runnable {


    internal class OutputExclusive {
        @CommandLine.Option(names = ["--output-override"], defaultValue = "false",
                            description = ["Override if output already exists", "Default is: '\${DEFAULT-VALUE}'", ""])
        var overrideExisting: Boolean = false

        @CommandLine.Option(names = ["--output-append-start"], defaultValue = "false",
                            description = ["Append the output on top of an existing file", "Default is: '\${DEFAULT-VALUE}'", ""])
        var appendToStart: Boolean = false
    }

    @CommandLine.Option(
            names = ["-o", "--output"],
            description = ["output changelog filename",
                ""],
    )
    private var outputFile: String? = null

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
    private var exclusive = OutputExclusive()

    @CommandLine.Option(names = ["-c", "--release-count"],
                        defaultValue = "1",
                        description = ["Last N releases to include into this changelog.",
                            "Default is: '\${DEFAULT-VALUE}'", ""])
    private var releaseCount = 1

    @CommandLine.Option(names = ["-t", "--commit-type"],
                        defaultValue = "fix,feat,perf",
                        description = ["Filter only those commits type", "Default is: '\${DEFAULT-VALUE}'", ""],
                        split = ",")
    private var commitType: List<String> = listOf(CommitType.BUG_FIX.prefix,
                                                  CommitType.FEAT.prefix,
                                                  CommitType.PERFORMANCE.prefix)

//    @CommandLine.Option(names = ["-f", "--from-tag"], description = ["Start changelog generation from this tag"])
//    private var fromTag: String? = null

    @CommandLine.Option(names = ["-n", "--release-name"], description = ["Provide the name of this release",
        "By default is automatically computed from last tag if you follow semantic versioning",
        "Option undefined means automatic release name'", ""])
    private var releaseName: String? = null

    @CommandLine.Option(names = ["--no-issue-link"], negatable = true, defaultValue = "true",
                        description = ["Append an issue link if an issue ID is found into short or full log message",
                            "Default is: '\${DEFAULT-VALUE}'",
                            ""])
    private var issueLink: Boolean = true

    @CommandLine.Option(names = ["-i", "--issue-url"], description = ["Tracker URL (Jira. github ...)",
        "If a card/issue ID is found is will be tail at the end",
        "Option undefined means we'll used github", ""])
    private var issueUrl: String? = null

    @CommandLine.Option(names = ["--issue-id-pattern"], description = ["a regexp to match an issue id",
        "If a card ID is found it will be append at the end of tracker url.",
        "Regex must contains 1 global group and 1 named capturing groups:",
        "   A global one used to identify an entirely issue id (ex: Closes: #1234) ",
        "   Second one is named 'ID', used to extract the id that will be appended after issueUrl (ex: 1234)",
        " Examples:",
        "    git: \"(#(?<ID>\\\\d+))\",",
        "    jira: \"(?<ID>JIRA-\\\\d+)\"",
        "    Strict conventional commit: \"(Closes: )#(?<ID>\\\\d+)\"",
        "    Advanced conventional commit: \"(([Cc][Ll][Oo][Ss][Ee][Ss][ \\t]*:[ \\t]*)?#(?<ID>\\\\d+))\"",
        "    Mix git and coco style: \"(([Cc][Ll][Oo][Ss][Ee][Ss][ \\t]*:[ \\t]*)?#?(?<ID>\\\\d+))\"",
        "Regex must be java compatible: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html",
        "Default is git style optionally with prefix 'Closes: ' '\${DEFAULT-VALUE}'", ""])
    private var issueIdRegex: String = defaultIssueRegex.pattern()

    @CommandLine.Option(names = ["-F", "--template-file"], description = ["Template file path",
        "Used to override the default changelog template. We use Mustache engine.",
        "Option undefined means we'll pick up the embedded one", ""])
    private var template: Path? = null

    @CommandLine.Option(names = ["--no-commit-link"], negatable = true, defaultValue = "true",
                        description = ["Add git commit URL for change log", "Default is: '\${DEFAULT-VALUE}'", ""])
    private var commitLink: Boolean = true

    @CommandLine.Option(names = ["-g", "--git-commit-url"],
                        description = [
                            "Remote url prefix to build commit link (github, gitlab ...)",
                            "Option undefined means we'll try to read from git remote (origin/master).", ""
                        ])
    private var gitCommitUrl: String? = null

    @CommandLine.Option(names = ["-r", "--remove-duplicate"],
                        description = [
                            "Remove duplicate commits based on description.",
                            "Commit IDs will be merged on same line.",
                            "Default is: '\${DEFAULT-VALUE}'.",
                            ""
                        ])
    private var removeDuplicate: Boolean = true

    @CommandLine.Option(names = ["-v", "--verbose"],
                        required = false,
                        description = ["print more information on console", "Default is: '\${DEFAULT-VALUE}'", ""])
    private var verbose: Boolean = false

    override fun run() {
        if (verbose) {
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
            val l = org.slf4j.LoggerFactory.getLogger(javaClass.packageName) as ch.qos.logback.classic.Logger
            l.level = Level.DEBUG
        }

        val params = GeneratorParams(template,
                                     exclusive.overrideExisting,
                                     exclusive.appendToStart,
                                     releaseName,
                                     outputFile,
                                     releaseCount,
                                     if (commitType.contains("*")) CommitType.values().toList()
                                     else commitType.map { CommitType.of(it) },
                                     commitLink,
                                     gitCommitUrl,
                                     issueLink,
                                     issueUrl,
                                     Pattern.compile(issueIdRegex, Pattern.DOTALL),
                                     removeDuplicate)
        ChangelogGenerator(params).run()
    }
}
//
//fun main(args: Array<String>) {
//    val commandLine = CommandLine(CoCoChaCmd())
//    commandLine.isOptionsCaseInsensitive = true
//    System.exit(commandLine.execute(*args))
//}