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

data class GeneratorParams(var template: Path?,
                           var releaseName: String?,
                           val outputFile: String?,
                           val releaseCount: Int = 1,
                           val filterCommitType: List<CommitType> = listOf(CommitType.BUG_FIX,
                                                                           CommitType.FEAT,
                                                                           CommitType.PERFORMANCE),
                           val gitRemoteUrl: String? = null,
                           val trackerUrl: String? = null) {
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

class ChangelogGenerator(val params: GeneratorParams) {
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
        logger.debug { "Release name is: '${params.releaseName}'" }
        val template = Mustache.compiler().compile(params.getTemplateReader())
        val releases = gitService.parseCommit(params.releaseName!!, tags, params.releaseCount)
        val md = template.execute(Releases(releases,
                                           params.gitRemoteUrl ?: gitService.getGitRemoteUrl(),
                                           params.trackerUrl))
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
        logger.info { "Generation finished: ${params.outputFile}" }
    }

    private fun buildReleaseName(relName: String?, tags: List<VersionTag>): String {
        return params.releaseName
                ?: if (tags.isEmpty()) VersionTag(0, 0, 1).toString()
                else tags.first().nextVersion().toString()
    }
}