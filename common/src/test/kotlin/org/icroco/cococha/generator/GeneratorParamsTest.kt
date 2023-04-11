package org.icroco.cococha.generator

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class GeneratorParamsTest {
    @ParameterizedTest
    @MethodSource("provide_comment")
    fun issueIdParsing(comment: String) {
        val matcher = defaultIssueRegex.matcher(comment)
        val soft = SoftAssertions()
        soft.assertThat(matcher.matches()).isTrue();
        soft.assertThat(matcher.group("ID")).isEqualTo("1234");

        soft.assertAll()
    }

    companion object {
        @JvmStatic
        fun provide_comment(): Stream<Arguments> {
            return Stream.of(
                    Arguments.of("closes: #1234"),
                    Arguments.of("Closes: #1234"),
                    Arguments.of("cloSes : #1234"),
                    Arguments.of("CLOSES : #1234"),
                    Arguments.of("#1234"),
                    Arguments.of("  #1234 "),
            )
        }
    }
}