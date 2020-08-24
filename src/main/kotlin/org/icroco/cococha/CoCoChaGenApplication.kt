package org.icroco.cococha

import picocli.CommandLine
import java.util.*

class CoCoChaGenApplication

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
class CoCoChaCmd : Runnable {

    @CommandLine.Option(names = ["-o", "--output"], description = ["output changelog filename",
        "Default value is '\${DEFAULT-VALUE}'"], defaultValue = "CHANGELOG.md")
    private var outputFile = "CHANGELOG.md"

    @CommandLine.Option(names = ["-c", "--release-count"], defaultValue = "1", description = ["Number of last releases to include into this changelog.",
        " default is '\${DEFAULT-VALUE}'"])
    private var releaseCount = 1

    @CommandLine.Option(names = ["-t", "--commit-type"], defaultValue = "feat,fix", description = ["Filter only those commits type",
        "Default value is '\${DEFAULT-VALUE}'"], split = ",")
    private var commitType: List<String> = listOf("feat,fix")

//    @CommandLine.Option(names = ["-s", "--start-tag"], description = ["Build changelog from this tag (default is from last tag)"])
//    private var startTag: String? = null

    override fun run() {

    }
}

fun main(args: Array<String>) {
    val commandLine = CommandLine(CoCoChaCmd())
    commandLine.isOptionsCaseInsensitive = true
    System.exit(commandLine.execute(*args))
}