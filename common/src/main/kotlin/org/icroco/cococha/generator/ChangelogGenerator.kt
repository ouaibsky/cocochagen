package org.icroco.cococha.generator

import com.samskivert.mustache.Mustache
import mu.KLogging
import org.icroco.cococha.generator.git.GitService
import org.icroco.cococha.generator.git.VersionTag
import java.io.FileReader
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern

val defaultIssueRegex: Pattern = Pattern.compile("(([Cc][Ll][Oo][Ss][Ee][Ss][ \t]*:[ \t]*)?#(?<ID>\\d+))",
                                                 Pattern.DOTALL)

data class GeneratorParams(var template: Path?,
                           var overrideExisting: Boolean = false,
                           var appendToStart: Boolean = false,
                           var releaseName: String?,
                           val outputFile: String?,
                           val releaseCount: Int = 1,
                           val filterCommitType: List<CommitType> = listOf(CommitType.BUG_FIX,
                                                                           CommitType.FEAT,
                                                                           CommitType.PERFORMANCE),
                           val addCommitLink: Boolean = true,
                           val gitCommitUrl: String? = null,
                           val addIssueLink: Boolean = true,
                           val issueUrl: String? = null,
                           val IssueIdRegex: Pattern = defaultIssueRegex,
                           val removeDuplicate: Boolean = true) {
    fun getTemplateReader(): Reader {
        val t = template?.let {
            if (!Files.exists(it)) {
                throw IllegalArgumentException("Template file doesn't exist: '$it'")
            }
            it
        }
        return if (t == null) InputStreamReader(ChangelogGenerator::class.java.getResourceAsStream(("/CHANGELOG.mustache")))
        else FileReader(t.toFile())
    }
}

class ChangelogGenerator(private val params: GeneratorParams) {
    private companion object : KLogging()

    private val gitService = GitService()

    fun run() {
        params.template = params.template?.let {
            if (!Files.exists(it)) {
                throw IllegalArgumentException("Template file doesn't exist: '$it'")
            }
            it
        }
        var tags = gitService.getTags()
        if (params.releaseCount >= 1) {
            tags = tags.take(params.releaseCount)
        }

        params.releaseName = buildReleaseName(tags)
        val gitUrl = if (params.addCommitLink) params.gitCommitUrl
                ?: gitService.getGitRemoteUrl() + "/commit/" else null
        val issueUrl = if (params.addIssueLink) params.issueUrl
                ?: gitService.getGitRemoteUrl() + "/issues/" else null

        val path: Path? = if (params.outputFile != null) Paths.get(params.outputFile).toAbsolutePath() else null
        logger.info { "Output file is: '${path?.toAbsolutePath() ?: "stdout"}'" }
        logger.info { "Output override existing is: '${params.overrideExisting}'" }
        logger.info { "Output append at start: '${params.appendToStart}'" }
        logger.info { "Release name is: '${params.releaseName}'" }
        logger.info { "Release Count is: '${params.releaseCount}'" }
        logger.info { "Filter commit log with: '${params.filterCommitType.joinToString(",") { it.prefix }}'" }
        logger.info { "Git commit URL: '${if (params.addCommitLink) gitUrl else "Disabled"}'" }
        logger.info { "Issue URL: '${if (params.addIssueLink) (issueUrl ?: "None") else "Disabled"}'" }
        logger.info { "Issue ID Regex: '${params.IssueIdRegex.pattern() ?: "None"}'" }
        logger.info { "Remove duplicate commits: '${params.removeDuplicate}'" }
        logger.info { "Mustache template: '${params.template ?: "embedded"}'" }
        val template = Mustache.compiler().compile(params.getTemplateReader())
        val releases = gitService.parseCommit(params.releaseName!!,
                                              tags,
                                              params.releaseCount,
                                              params.filterCommitType,
                                              params.IssueIdRegex,
                                              params.removeDuplicate)
        val md = template.execute(Releases(releases, gitUrl, issueUrl))

        if (path == null) {
            println("--------------- CHANGELOG BEGIN ------------------")
            println(md)
            println("----------------CHANGELOG END -----------------")
        } else {
            if (params.appendToStart && params.overrideExisting) {
                throw IllegalArgumentException("Option to override and append are exclusive, both cannot be true")
            }
            if (path.toFile().isDirectory) {
                throw IllegalArgumentException("Output cannot be a directory: '$path'")
            }
            path.parent.toFile().mkdirs()
            if (Files.notExists(path.parent)) {
                throw IllegalArgumentException("Parent directory doesn't exist: '${path.parent.toAbsolutePath()}'")
            }
            if (Files.exists(path) && !params.overrideExisting) {
                throw IllegalArgumentException("File: '${path.toAbsolutePath()}' already exists. Set the right option to force overriding or change filename")
            }
            if (Files.exists(path) && params.appendToStart) {
                val previousFile = Files.readString(path)
                Files.writeString(path, md + """
                    -----
                    
                """.trimIndent(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                Files.writeString(path, previousFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            } else {
                Files.writeString(path,
                                  md,
                                  StandardOpenOption.CREATE,
                                  StandardOpenOption.TRUNCATE_EXISTING)
            }
            logger.info { "Generation finished: ${path.toAbsolutePath() ?: "stdout"}" }
        }
    }

    private fun buildReleaseName(tags: List<VersionTag>): String {
        logger.debug {
            "create release name, manual name: '${params.releaseName}', tags: '${
                tags.take(10).joinToString(",") { it.toString() }
            }'"
        }
        val name = if (params.releaseName == null) {
            if (tags.isEmpty())
                VersionTag(0, 0, 1).toString()
            else
                tags.first().nextVersion().toString()
        } else {
            null
        }
        logger.debug { "Final Release Name: '${name}'" }
        return name
                ?: throw IllegalArgumentException("Release name can not be null and cannot be computed from semantic version found in last tag")
    }
}


