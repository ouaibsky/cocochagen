package org.icroco.cococha.generator.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.jupiter.api.Test


class GitServiceTest {

    @Test
    fun getGitRemoteUrl() {
        val builder = FileRepositoryBuilder()
        val repository = builder.readEnvironment()
                .findGitDir()
                .build()

        var git = Git(repository)

        repository.remoteNames.forEach { println("Remote repo: '$it'") }
        println("DEfault Branch: '${repository.branch}'")
        val trackingStatus = BranchTrackingStatus.of(repository, "master")

        println("remote tracking: " + trackingStatus.remoteTrackingBranch.replace("refs/remotes/", ""))
        println("getGitRemoteUrl: " + GitService().getGitRemoteUrl())

    }

}