package org.icroco.cococha

import com.samskivert.mustache.Mustache
import mu.KLogging
import org.icroco.cococha.git.GitService
import org.icroco.cococha.git.VersionTag
import java.io.FileReader
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern

val defaultIssueRegex = Pattern.compile("(?<R>([Cc][Ll][Oo][Ss][Ee][Ss][ \t]*:[ \t]*)?#(?<ID>\\d+))", Pattern.DOTALL)

data class GeneratorParams(var template: Path?,
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
                           val IssueIdRegex: Pattern = defaultIssueRegex) {
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

        params.releaseName = buildReleaseName(params.releaseName, tags)
        val gitUrl = if (params.addCommitLink) params.gitCommitUrl
                ?: gitService.getGitRemoteUrl() + "/commit/" else null
        val issueUrl = if (params.addIssueLink) params.issueUrl
                ?: gitService.getGitRemoteUrl() + "/issues/" else null

        logger.info { "Release name is: '${params.releaseName}'" }
        logger.info { "Filter commit log with: '${params.filterCommitType.joinToString(",") { it.prefix }}'" }
        logger.info { "Git commit URL: '${if (params.addCommitLink) gitUrl else "Disabled"}'" }
        logger.info { "Issue URL: '${if (params.addIssueLink) (issueUrl ?: "None") else "Disabled"}'" }
        logger.info { "Issue ID Regex: '${params.IssueIdRegex?.pattern() ?: "None"}'" }
        val template = Mustache.compiler().compile(params.getTemplateReader())
        val releases = gitService.parseCommit(params.releaseName!!,
                                              tags,
                                              params.releaseCount,
                                              params.filterCommitType,
                                              params.IssueIdRegex)
        val md = template.execute(Releases(releases, gitUrl, issueUrl))

        if (params.outputFile == null) {
            println("--------------- CHANGELOG BEGIN ------------------")
            println(md)
            println("----------------CHANGELOG END -----------------")
        } else {
            Files.writeString(Paths.get(params.outputFile),
                              md,
                              StandardOpenOption.CREATE,
                              StandardOpenOption.TRUNCATE_EXISTING)
        }
        logger.info { "Generation finished: ${params.outputFile ?: "stdout"}" }
    }

    private fun buildReleaseName(relName: String?, tags: List<VersionTag>): String {
        return params.releaseName
                ?: if (tags.isEmpty()) VersionTag(0, 0, 1).toString()
                else tags.first().nextVersion().toString()
    }
}