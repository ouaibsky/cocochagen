package org.icroco.cocochagen;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.icroco.cococha.generator.ChangelogGenerator;
import org.icroco.cococha.generator.ChangelogGeneratorKt;
import org.icroco.cococha.generator.CommitType;
import org.icroco.cococha.generator.GeneratorParams;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Generate a Changelog based on Conventional Commit format.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = false)
public class CoCoChaGenMojo
        extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * output changelog filename. If null result will display on stdout
     */
    @Parameter(property = "cococha.outputFile", defaultValue = "CHANGELOG.md")
    String outputFile = "CHANGELOG.md";

    /**
     * Override if output already exists", "Default is: 'false'"
     */
    @Parameter(property = "cococha.overrideExisting", defaultValue = "false")
    Boolean overrideExisting = false;

    /**
     * Append the output on top of an existing file, "Default is: 'false'"
     */
    @Parameter(property = "cococha.appendToStart", defaultValue = "false")
    Boolean appendToStart = false;

    /**
     * Last N releases to include into this changelog
     */
    @Parameter(property = "cococha.releaseCount", defaultValue = "1")
    Integer releaseCount = 1;

    /**
     * Filter only those commits type", "Default is: <b>'fix,feat,perf'</b>.<br/>
     * '<b>*</b>' is supported in order to include all type of commits
     */
    @Parameter(property = "cococha.filterCommitTypes", defaultValue = "fix,feat,perf")
    List<String> filterCommitTypes = List.of(CommitType.BUG_FIX.getPrefix(),
            CommitType.FEAT.getPrefix(),
            CommitType.PERFORMANCE.getPrefix());

    /**
     * Provide the name of this release.<br/>
     * By default is automatically computed from last tag if you follow semantic versioning.<br/>
     * Option undefined means automatic release name.
     */
    @Parameter(property = "cococha.releaseName")
    String releaseName = null;

    /**
     * Append an issue link if an issue ID is found into short or full log message.
     * Default is: 'true'
     */
    @Parameter(property = "cococha.addIssueLink", defaultValue = "true")
    Boolean addIssueLink = true;

    /**
     * Tracker URL (Jira. github ...).
     * If a card/issue ID is found is will be tail at the end.
     * Option undefined means we'll used github.
     */
    @Parameter(property = "cococha.issueUrl")
    String issueUrl = null;

    /**
     * If a card ID is found it will be append at the end of tracker url.<br/>
     * Regex must contains 1 global group and 1 named capturing groups:<br/>
     * <ul style="PADDING-LEFT: 12px">
     *     <li>A global one used to identify an entirely issue id (ex: Closes: #1234).</li>
     *     <li>Second one is named 'ID', used to extract the id that will be appended after issueUrl (ex: 1234),</li>
     * </ul>
     * <br/>
     * Examples:"<br/>
     * <ul style="PADDING-LEFT: 12px">
     *     <li>git: \"(#(?<ID>\\\\d+))\"</li>
     *     <li>jira: \"(?<ID>JIRA-\\\\d+)\"</li>
     *     <li>Strict conventional commit: \"(Closes: )#(?<ID>\\\\d+)\"</li>
     *     <li>Advanced conventional commit: \"(([Cc][Ll][Oo][Ss][Ee][Ss][ \\t]*:[ \\t]*)?#(?<ID>\\\\d+))\"</li>
     * </ul>
     * <br/>
     * <div>
     * Regex must be java compatible: <a href="https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html">Oracle</a><br/>
     * Default is git style optionally with prefix 'Closes: '(([Cc][Ll][Oo][Ss][Ee][Ss][ 	]*:[ 	]*)?#(?<ID>\d+))'
     * </div>
     */
    @Parameter(property = "cococha.issueIdRegex")
    String issueIdRegex = ChangelogGeneratorKt.getDefaultIssueRegex().pattern();

    /**
     * Template file path (mustache is used as template engine).<br/>
     * Used to override the default changelog template. We use <b>Mustache</b> engine.<br/>
     * Option undefined means we'll pick up the embedded one.
     */
    @Parameter(property = "cococha.template")
    File template = null;

    /**
     * Add git commit URL for change log". Default is: 'true'
     */
    @Parameter(property = "cococha.addCommitLink", defaultValue = "true")
    Boolean addCommitLink = true;

    /**
     * Remove duplicate commit description. <br/>
     * Issue Id or Commit Id will be merged on same line.
     */
    @Parameter(property = "cococha.removeDuplicate", defaultValue = "true")
    Boolean removeDuplicate = true;

    /**
     * Remote url prefix to build commit link (github, gitlab ...).<br/>
     * Option undefined means we'll try to read from git remote (origin/master).
     */
    @Parameter(property = "cococha.gitCommitUrl")
    String gitCommitUrl = null;

    /**
     * For to fetch all available tags. Depending of you repository it can be long,
     */
    @Parameter(property = "cococha.fetchTags", defaultValue = "false")
    Boolean fetchTags = false;

    public void execute() throws MojoExecutionException {
        try {
            GeneratorParams params = new GeneratorParams(
                    template == null ? null : template.toPath(),
                    overrideExisting,
                    appendToStart,
                    releaseName,
                    ofNullable(project).map(MavenProject::getBasedir)
                            .orElseGet(() -> new File(".")).toString() + "/" + outputFile,
                    releaseCount,
                    filterCommitTypes.contains("*")
                            ? Arrays.asList(CommitType.values().clone())
                            : filterCommitTypes.stream()
                            .map(v -> CommitType.Companion.of(v, null)).collect(Collectors.toList()),
                    addCommitLink,
                    gitCommitUrl,
                    addIssueLink,
                    issueUrl,
                    Pattern.compile(issueIdRegex, Pattern.DOTALL),
                    removeDuplicate,
                    fetchTags);
            new ChangelogGenerator(params).run();
        } catch (Exception e) {
            throw new MojoExecutionException("Cococha generator failed", e);
        }
    }
}
