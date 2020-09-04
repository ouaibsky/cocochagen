package org.icroco.cococha

import mu.KLogging
import org.icroco.cococha.git.GitService
import org.icroco.cococha.git.VersionTag

data class GeneratorParams(val releaseName: String?,
                           val outputFile: String?,
                           val releaseCount: Int = 1,
                           val filterCommitType: List<CommitType> = listOf(CommitType.BUG_FIX, CommitType.FEAT, CommitType.PERFORMANCE)
)

class ChangelogGenerator(val params: GeneratorParams) {
    private companion object : KLogging()

    private val gitService = GitService()

    fun run() {
        var tags = gitService.getTags()
        if (params.releaseCount >= 1) {
            tags = tags.take(params.releaseCount)
        }

        val relName = buildReleaseName(params.releaseName, tags)
        logger.debug {"Release name is: '$relName'" }
        for (release in gitService.parseCommit(tags)) {
            println(release)
        }
    }

    private fun buildReleaseName(relName: String?, tags: List<VersionTag>): String {
        return params.releaseName
                ?: if (tags.isEmpty()) VersionTag(0, 0, 1).toString()
                else tags.first().nextVersion().toString()
    }
}