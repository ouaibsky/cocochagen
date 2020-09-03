package org.icroco.cococha

import java.time.LocalDate


enum class CommitType(val displayPriority: Int, val prefix: String, val fullName: String) : Comparable<CommitType> {
    BUILD(5, "build", "Builds"),
    TEST(6, "test", "Tests"),
    CI(9, "ci", "Continuous Integration"),
    CHORE(8, "chore", "Chores"),
    DOCS(7, "docs", "Documentation"),
    FEAT(1, "feat", "Features"),
    BUG_FIX(0, "fix", "Bug Fixes"),
    PERFORMANCE(2, "perf", "Performance Improvements"),
    STYLE(10, "style", "Styles"),
    REVERT(3, "revert", "Reverts"),
    REFACTOR(4, "refactor", "Code Refactoring");

    companion object {
        fun of(value: String): CommitType {
            for (type in values()) {
                if (type.prefix == value) {
                    return type
                }
            }
            throw IllegalStateException(
                    value + " commit type is not supported by " + CommitType::class.java.simpleName)
        }
    }
}
data class CommitDesc(val component: String, val description: String, val trackerId: String, val commitId: String);
data class Category(val type: CommitType, val cards: List<CommitDesc>)
data class Release(val date: LocalDate, val categories: List<Category>)
data class Releases(val releases: List<Release>)
