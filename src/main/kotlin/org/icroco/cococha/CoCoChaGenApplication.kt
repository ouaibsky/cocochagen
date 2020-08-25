package org.icroco.cococha

import org.icroco.cococha.git.GitService
import org.icroco.cococha.git.SemanticVersion
import picocli.CommandLine
import java.util.*

class CoCoChaGenApplication {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val commandLine = CommandLine(CoCoChaCmd())
            commandLine.isOptionsCaseInsensitive = true
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
    private val gitService = GitService()

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

    @CommandLine.Option(names = ["-s", "--start-tag"], required = false, description = ["Tag to start looking commits",
        "Default value is '\${DEFAULT-VALUE}'"])
    private var startTag: String? = null

    override fun run() {
        if (listTag) {
            gitService.getSemanticVersionTag().forEach { println(it) }
        }
        val semVer = if (startTag == null) gitService.getSemanticVersionTag().first() else (SemanticVersion(-1, -1, -1, startTag!!))
        println("StartTag: $semVer")
    }
}
//
//fun main(args: Array<String>) {
//    val commandLine = CommandLine(CoCoChaCmd())
//    commandLine.isOptionsCaseInsensitive = true
//    System.exit(commandLine.execute(*args))
//}