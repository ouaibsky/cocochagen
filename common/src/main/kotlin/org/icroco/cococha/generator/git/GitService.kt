package org.icroco.cococha.generator.git

import mu.KLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.icroco.cococha.generator.CommitDesc
import org.icroco.cococha.generator.CommitType
import org.icroco.cococha.generator.Release
import java.io.File
import java.util.*
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

    fun isSemantic(): Boolean {
        return major >= 0 && minor >= 0 && build >= 0
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
        return if (isSemantic())
            VersionTag(if (plusMajor) major + 1 else major,
                       if (plusMinor) minor + 1 else minor,
                       if (plusBuild) build + 1 else build,
                       ref)
        else throw IllegalArgumentException("Can not create automatic version number from non semantic version: '$major.$minor.$build'")
    }
}

class GitService(baseDir: File? = null) {
    private companion object : KLogging()

    private val repository: Repository = RepositoryBuilder()
            .readEnvironment()
            .findGitDir(baseDir ?: File("").absoluteFile)
            .build()
    private val git = Git(repository)
    private val tagPattern = Pattern.compile("refs/tags/(v?(\\d+)\\.(\\d+)\\.(\\d+))")
    private val typePattern =
            Pattern.compile("^\\s*(?<T>${CommitType.buildPattern()})\\s*([(](?<C>\\w*)[)]\\s*)?:\\s*(?<D>.*)")

    fun getTags(): List<VersionTag> {
//        git.fetch().setTagOpt(TagOpt.FETCH_TAGS).call();
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

    private fun getCommitRange(releaseName: String,
                               from: ObjectId,
                               to: ObjectId,
                               filterCommitType: List<CommitType>,
                               issueIdRegex: Pattern,
                               removeDuplicate: Boolean): Release {

//        val rw = RevWalk(repository)
//        val ro = rw.parseCommit(from)
//        val time = ro.authorIdent.`when`
//        println("${time}")

        val log = git.log()
        val commits = log.addRange(from, to)
                .call()
                .mapNotNull { rm -> toConventionalCommit(rm, issueIdRegex) }
                .groupBy { it.type }
                .toSortedMap(CommitType.sortByPrio)
                .mapValues { e ->
                    val cLogs = e.value.filter { cd -> filterCommitType.contains(cd.type) }
                    if (removeDuplicate) {
                        val uQc = mutableMapOf<String, CommitDesc>()
                        cLogs.forEach { cd ->
                            uQc.compute(cd.description) { _, v -> v?.addCommitIds(cd.commitIds) ?: cd }
                        }
                        uQc.values
                    } else {
                        cLogs
                    }
                            .sortedBy { it.component }
                }
                .filter { e -> e.value.isNotEmpty() }
        val parseCommit = repository.parseCommit(to)
        val authorDate = parseCommit.authorIdent.getWhen()
        val authorTimeZone = parseCommit.authorIdent.timeZone.toZoneId()
        return Release(releaseName,
                       authorDate.toInstant().atZone(authorTimeZone).toLocalDate(),
                       commits.mapKeys { it.key.fullName })
    }

    private fun toConventionalCommit(rm: RevCommit, issueIdRegex: Pattern): CommitDesc {
        logger.debug { "Found commit log: '${rm.shortMessage}'" }
        val matcher = typePattern.matcher(rm.shortMessage)
        var cType = CommitType.UNKNOWN
        var cComponent: String? = null
        var cDesc = rm.shortMessage

        if (matcher.matches()) {
            cDesc = matcher.group("D")
            cType = CommitType.of(matcher.group("T"))
            cComponent =
                    matcher.group("C")
                            ?.replace("_", " ")
                            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        cDesc = (cDesc.ifBlank { rm.fullMessage?.lines()?.first() ?: "No Commit Msg" }).trim()
        val pair = getIds(cDesc, issueIdRegex.matcher(rm.shortMessage), issueIdRegex.matcher(rm.fullMessage))
        cDesc = pair.first.trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        cDesc = if (cDesc.endsWith(".")) cDesc else "${cDesc}."

        return CommitDesc(cType, cComponent, cDesc, pair.second, mutableSetOf(rm.id.abbreviate(8).name()))
    }

    private fun getIds(desc: String, matcherShort: Matcher, matcherFull: Matcher): Pair<String, Set<String>> {
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
        val currentBranch = repository.branch
        val trackingStatus = BranchTrackingStatus.of(repository, currentBranch)
        val remoteTrackingBr: String? = trackingStatus?.remoteTrackingBranch;
        val remoteName = if (remoteTrackingBr == null) {
            val remotes = repository.remoteNames.toSet()
            if (remotes.size > 1) {
                "origin"
            } else {
                remotes.first()
            }
        } else {
            remoteTrackingBr.split("/")[2]
        }
        return repository.config.getString("remote", remoteName, "url")?.replace("\\.git\$".toRegex(), "")
    }

    fun parseCommit(relName: String,
                    tags: List<VersionTag>,
                    releaseCount: Int,
                    filterCommitType: List<CommitType>,
                    issueIdRegex: Pattern,
                    removeDuplicate: Boolean = true): List<Release> {
        var to = repository.resolve(Constants.HEAD)
        var name = relName
        val releases = mutableListOf<Release>()
        for (t in tags) {
            val from = repository.refDatabase.peel(t.ref).getSafeObjectId()
            releases.add(getCommitRange(name, from, to, filterCommitType, issueIdRegex, removeDuplicate))
            to = from
            name = t.toString()
        }
        if (tags.isEmpty() || releases.size < releaseCount) {
            val from = getOldLog()
            name = if (tags.isEmpty()) relName else tags.last().toString()
            releases.add(getCommitRange(name, from, to, filterCommitType, issueIdRegex, removeDuplicate))
            // FIXME: first commit of git log history is omitted
        }

        return releases.filter { r -> r.messages.isNotEmpty() }
    }

}