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
    REFACTOR(4, "refactor", "Code Refactoring"),
    UNKNOWN(4, "unknown", "Others");

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
                      val trackerId: String?,
                      val commitId: String);

data class Release(val name: String, val date: LocalDate, val categories: Map<String, List<CommitDesc>>)

data class Releases(val releases: List<Release>,
                    val gitUrl: String?,
                    val trackerUrl: String?,
                    val header: String? = null,
                    val footer: String? = null)
