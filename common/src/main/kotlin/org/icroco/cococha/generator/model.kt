package org.icroco.cococha.generator

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
    REFACTOR(4, "refactor", "Code Refactoring"),
    UNKNOWN(1000, "unknown", "Others");

    companion object {
        public fun of(value: String, defaultValue: CommitType? = null): CommitType {
            for (type in values()) {
                if (type.prefix.equals(value, true)) {
                    return type
                }
            }
            return defaultValue
                    ?: throw IllegalStateException("Unknown commit type: '$value'. values: '${buildPattern()}'")
        }

        fun buildPattern(): String {
            return values().joinToString("|") { it.prefix }
        }

        val sortByPrio = compareBy<CommitType>() { it.displayPriority }
    }
}

data class CommitDesc(val type: CommitType,
                      val component: String?,
                      val description: String,
                      val issueIds: Set<String>,
                      var commitIds: Set<String>) {
    fun addCommitIds(cId: Collection<String>): CommitDesc {
        val newV = commitIds.toMutableSet()
        newV.addAll(cId)
        commitIds = newV
        return this
    }
}


data class Category(val name: String, val messages: List<CommitDesc>)

data class Release(val name: String, val date: LocalDate, val messages: Map<String, List<CommitDesc>>) {
    fun categories(): List<Category> {
        return messages.entries.map { e -> Category(e.key, e.value) }
    }
}

data class Releases(val releases: List<Release>,
                    val gitUrl: String?,
                    val issueUrl: String?,
                    val header: String? = null,
                    val footer: String? = null)
