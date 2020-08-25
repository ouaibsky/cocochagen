package org.icroco.cococha.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File
import java.io.IOException
import java.util.regex.Pattern


fun Ref.getSafeObjectId(): ObjectId? {
    return if (peeledObjectId != null) {
        peeledObjectId
    } else objectId
}


data class SemanticVersion(val major: Int, val minor: Int, val build: Int, val label: String) : Comparable<SemanticVersion> {

    override fun toString(): String {
        return if (major == -1) label else "v$major.$minor.$build"
    }

    override fun compareTo(other: SemanticVersion): Int {
        if (major == other.major) {
            if (minor == other.minor) {
                return build - other.build
            } else {
                return minor - other.minor
            }
        }
        return major - other.major
    }
}

class GitService(private val baseDir: File? = null) {
    val repository: Repository = RepositoryBuilder()
            .readEnvironment()
            .findGitDir(baseDir ?: File("").absoluteFile)
            .build()
    val git = Git(repository)

    fun getSemanticVersionTag(): List<SemanticVersion> {
        val pattern = Pattern.compile("refs/tags/(v(\\d+)\\.(\\d+)\\.(\\d+))")
        return git.tagList()
                .call()
                .asSequence()
                .sortedBy { ref -> repository.parseCommit(ref.getSafeObjectId()).commitTime }
                .map { r -> pattern.matcher(r.name) }
                .filter { p -> p.matches() }
                .map { m -> SemanticVersion(m.group(2).toInt(), m.group(3).toInt(), m.group(4).toInt(), m.group(1)) }
                .toList()
    }
}