package org.icroco.cococha.git

import mu.KLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.icroco.cococha.CommitDesc
import org.icroco.cococha.CommitType
import org.icroco.cococha.Release
import java.io.File
import java.util.regex.Pattern


fun Ref.getSafeObjectId(): ObjectId {
    return if (peeledObjectId != null) {
        peeledObjectId
    } else objectId
}


data class VersionTag(val major: Int,
                      val minor: Int,
                      val build: Int,
                      val ref: Ref? = null) : Comparable<VersionTag> {

    constructor(ref: Ref) : this(-1, -1, -1, ref) {
    }

    private fun isSemantic(): Boolean {
        return major >= 0 && minor >= 0 && minor >= 0
    }

    override fun toString(): String {
        return if (isSemantic()) "v$major.$minor.$build" else ref?.name?.removePrefix("refs/tags/") ?: "NotYetDefined"
    }

    override fun compareTo(other: VersionTag): Int {
        if (major == other.major) {
            return if (minor == other.minor) {
                build - other.build
            } else {
                minor - other.minor
            }
        }
        return major - other.major
    }

    fun nextVersion(plusMajor: Boolean = false, plusMinor: Boolean = false, plusBuild: Boolean = true): VersionTag {
        return if (isSemantic()) VersionTag(
                if (plusMajor) major + 1 else major,
                if (plusMinor) minor + 1 else minor,
                if (plusBuild) build + 1 else build,
                ref)
        else throw IllegalArgumentException("Can not create automatuc version number fron non semantic version")
    }
}

class GitService(baseDir: File? = null) {
    private companion object : KLogging()

    private val repository: Repository = RepositoryBuilder()
        .readEnvironment()
        .findGitDir(baseDir ?: File("").absoluteFile)
        .build()
    private val git = Git(repository)
    private val tagPattern = Pattern.compile("refs/tags/(v(\\d+)\\.(\\d+)\\.(\\d+))")
    private val typePattern = Pattern.compile("^\\s*(?<T>${CommitType.buildPattern()})\\s*([(](?<C>\\w*)[)]\\s*)?:\\s*(?<D>.*)")

    fun getTags(): List<VersionTag> {
        return git.tagList()
            .call()
            .asSequence()
            .sortedByDescending { ref -> repository.parseCommit(ref.getSafeObjectId()).commitTime }
            .map { r: Ref ->
                val m = tagPattern.matcher(r.name)
                logger.debug { "Found tag: '$r'" + (if (m.matches()) "" else " Non conventional tag, will be ignored for release auto naming") }
                if (m.matches()) VersionTag(m.group(2).toInt(), m.group(3).toInt(), m.group(4).toInt(), r)
                else VersionTag(r)
            }
//                .filter { (_, m) -> m.matches() }
//                .map { (r, m) -> VersionTag( m.group(2).toInt(), m.group(3).toInt(), m.group(4).toInt()) }
            .toList()
    }

    fun getTagRef(tagName: String): VersionTag {
        return VersionTag(git.tagList()
                              .call()
                              .asSequence()
                              .filter { t -> t.name == tagName }
                              .first())
        // TODO: manage if not found.
    }

    private fun getCommitRange(releaseName: String, from: ObjectId, to: ObjectId): Release {

//        val rw = RevWalk(repository)
//        val ro = rw.parseCommit(from)
//        val time = ro.authorIdent.`when`
//        println("${time}")

        val log = git.log()
        val commits = log.addRange(from, to)
            .call()
            .map { rm ->
                logger.debug { "Found commit log: '${rm.shortMessage}'" }
                val matcher = typePattern.matcher(rm.shortMessage)
                if (matcher.matches()) {
                    val desc = matcher.group("D")
                    CommitDesc(CommitType.of(matcher.group("T")),
                               matcher.group("C").replace("_", " "),
                               if (desc.isBlank()) rm.fullMessage.lines().first() else desc.trim(),
                               null,
                               rm.id.abbreviate(8).name())
                } else {
                    CommitDesc(CommitType.UNKNOWN, null, rm.shortMessage, null, rm.id.abbreviate(8).name())
                }
            }.groupBy { it.type }
            .toSortedMap(CommitType.sortByPrio)
            .mapValues { e -> e.value.sortedBy { it.component } } // TODO: remove duplicate Desc / Tracker
        val parseCommit = repository.parseCommit(to)
        val authorDate = parseCommit.authorIdent.getWhen()
        val authorTimeZone = parseCommit.authorIdent.timeZone.toZoneId()
        return Release(releaseName,
                       authorDate.toInstant().atZone(authorTimeZone).toLocalDate(),
                       commits.mapKeys { it.key.fullName })
    }

    fun getCommitRange(releaseName: String, from: Ref): Release {
        return getCommitRange(releaseName, from.getSafeObjectId(), repository.resolve(Constants.HEAD))
    }

    private fun getOldLog(): ObjectId {
        val call = git.log().setMaxCount(1000).call()
        return call.last()
    }

    fun getGitRemoteUrl(): String? {
        return repository.config.getString("remote", "origin", "url")?.replace("\\.git\$".toRegex(), "")
    }

    fun parseCommit(relName: String, tags: List<VersionTag>, releaseCount: Int): List<Release> {
        var to = repository.resolve(Constants.HEAD)
        var name = relName
        val releases = mutableListOf<Release>()
        for (t in tags) {
            val from = repository.refDatabase.peel(t.ref).getSafeObjectId()
            releases.add(getCommitRange(name, from, to))
            to = from
            name = t.toString()
        }
        if (tags.isEmpty() || releases.size < releaseCount) {
            val from = getOldLog()
            name = if (tags.isEmpty()) relName else tags.last().toString()
            releases.add(getCommitRange(name, from, to))
            // FIXME: first commit of git history is omitted
        }

        return releases
    }

}