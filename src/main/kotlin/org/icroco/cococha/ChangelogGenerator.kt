package org.icroco.cococha

import com.samskivert.mustache.Mustache
import mu.KLogging
import org.icroco.cococha.git.GitService
import org.icroco.cococha.git.VersionTag
import java.io.InputStreamReader

data class GeneratorParams(val releaseName: String?,
                           val outputFile: String?,
                           val releaseCount: Int = 1,
                           val filterCommitType: List<CommitType> = listOf(CommitType.BUG_FIX,
                                                                           CommitType.FEAT,
                                                                           CommitType.PERFORMANCE),
                           val gitRemoteUrl: String? = null,
                           val trackerUrl: String? = null)

class ChangelogGenerator(val params: GeneratorParams) {
    private companion object : KLogging()

    private val gitService = GitService()

    fun run() {
        var tags = gitService.getTags()
        if (params.releaseCount >= 1) {
            tags = tags.take(params.releaseCount)
        }

        val relName = buildReleaseName(params.releaseName, tags)
        logger.debug { "Release name is: '$relName'" }
        val template = Mustache.compiler().compile(InputStreamReader(ChangelogGenerator.javaClass.getResourceAsStream(("/CHANGELOG.mustache"))))
        val releases = gitService.parseCommit(relName, tags, params.releaseCount)
        val md = template.execute(Releases(releases,
                                           params.gitRemoteUrl ?: gitService.getGitRemoteUrl(),
                                           params.trackerUrl))
        println("---------------------------------")
        println(md)
        println("---------------------------------")
    }

    private fun buildReleaseName(relName: String?, tags: List<VersionTag>): String {
        return params.releaseName
                ?: if (tags.isEmpty()) VersionTag(0, 0, 1).toString()
                else tags.first().nextVersion().toString()
    }
}