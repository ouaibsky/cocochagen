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


data class SemanticVersion(val major: Int, val minor: Int, val build: Int, val label: String? = null) : Comparable<SemanticVersion> {

    constructor(label: String) : this(-1, -1, -1, label) {
    }

    override fun toString(): String {
        return label ?: "v$major.$minor.$build"
    }

    override fun compareTo(other: SemanticVersion): Int {
        if (major == other.major) {
            return if (minor == other.minor) {
                build - other.build
            } else {
                minor - other.minor
            }
        }
        return major - other.major
    }
}

class GitService(private val baseDir: File? = null) {
    private val repository: Repository = RepositoryBuilder()
            .readEnvironment()
            .findGitDir(baseDir ?: File("").absoluteFile)
            .build()
    private val git = Git(repository)
    private val pattern = Pattern.compile("refs/tags/(v(\\d+)\\.(\\d+)\\.(\\d+))")

    fun getSemanticVersionTag(): List<SemanticVersion> {
        return git.tagList()
                .call()
                .asSequence()
                .sortedByDescending { ref -> repository.parseCommit(ref.getSafeObjectId()).commitTime }
                .map { r -> pattern.matcher(r.name) }
                .filter { p -> p.matches() }
                .map { m -> SemanticVersion(m.group(2).toInt(), m.group(3).toInt(), m.group(4).toInt()) }
                .toList()
    }
}