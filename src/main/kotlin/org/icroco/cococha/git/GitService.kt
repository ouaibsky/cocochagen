package org.icroco.cococha.git

import mu.KLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.icroco.cococha.CommitDesc
import org.icroco.cococha.CommitType
import org.icroco.cococha.Release
import java.io.File
import java.util.regex.Matcher
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

    private fun getCommitRange(releaseName: String,
                               from: ObjectId,
                               to: ObjectId,
                               filterCommitType: List<CommitType>,
                               issueIdRegex: Pattern): Release {

//        val rw = RevWalk(repository)
//        val ro = rw.parseCommit(from)
//        val time = ro.authorIdent.`when`
//        println("${time}")

        val log = git.log()
        val commits = log.addRange(from, to)
            .call()
            .mapNotNull { rm ->
                logger.debug { "Found commit log: '${rm.shortMessage}'" }
                val matcher = typePattern.matcher(rm.shortMessage)

                if (matcher.matches()) {
                    var desc = matcher.group("D")
                    desc = (if (desc.isBlank()) rm.fullMessage?.lines()?.first() ?: "No Commit Msg" else desc).trim()
                    val pair = getIds(desc, issueIdRegex.matcher(rm.shortMessage), issueIdRegex.matcher(rm.fullMessage))
                    desc = pair.first.trim()
                    desc = if (desc.endsWith(".")) desc else "${desc}."
                    CommitDesc(CommitType.of(matcher.group("T")),
                               matcher.group("C")?.replace("_", " "),
                               desc,
                               pair.second,
                               rm.id.abbreviate(8).name())
                } else {
                    CommitDesc(CommitType.UNKNOWN, null, rm.shortMessage, emptySet(), rm.id.abbreviate(8).name())
                }
            }
            .groupBy { it.type }
            .toSortedMap(CommitType.sortByPrio)
            .mapValues { e ->
                e.value
                    .filter { cd -> filterCommitType.contains(cd.type) }
                    .sortedBy { it.component }
            }.filter { e -> e.value.isNotEmpty() }
        // TODO: remove duplicate Desc
        val parseCommit = repository.parseCommit(to)
        val authorDate = parseCommit.authorIdent.getWhen()
        val authorTimeZone = parseCommit.authorIdent.timeZone.toZoneId()
        return Release(releaseName,
                       authorDate.toInstant().atZone(authorTimeZone).toLocalDate(),
                       commits.mapKeys { it.key.fullName })
    }

    fun getIds(desc: String, matcherShort: Matcher, matcherFull: Matcher): Pair<String, Set<String>> {
        var newDesc = desc
        val ids = mutableSetOf<String>()
        while (matcherShort.find()) {
            newDesc = newDesc.replace(matcherShort.group(0), "")
            ids.add(matcherShort.group("ID"))
        }
        while (matcherFull.find()) {
            ids.add(matcherFull.group("ID"))
        }
        return Pair(newDesc, ids.toSortedSet())
    }

    private fun getOldLog(): ObjectId {
        val call = git.log().setMaxCount(1000).call()
        return call.last()
    }

    fun getGitRemoteUrl(): String? {
        return repository.config.getString("remote", "origin", "url")?.replace("\\.git\$".toRegex(), "")
    }

    fun parseCommit(relName: String,
                    tags: List<VersionTag>,
                    releaseCount: Int,
                    filterCommitType: List<CommitType>,
                    issueIdRegex: Pattern): List<Release> {
        var to = repository.resolve(Constants.HEAD)
        var name = relName
        val releases = mutableListOf<Release>()
        for (t in tags) {
            val from = repository.refDatabase.peel(t.ref).getSafeObjectId()
            releases.add(getCommitRange(name, from, to, filterCommitType, issueIdRegex))
            to = from
            name = t.toString()
        }
        if (tags.isEmpty() || releases.size < releaseCount) {
            val from = getOldLog()
            name = if (tags.isEmpty()) relName else tags.last().toString()
            releases.add(getCommitRange(name, from, to, filterCommitType, issueIdRegex))
            // FIXME: first commit of git log history is omitted
        }

        return releases.filter { r -> r.messages.isNotEmpty() }
    }

}