# Read Me First
[Convention Commit](https://www.conventionalcommits.org/) Changelog Generator

# Getting Started

[look at output example](examples/CHANGELOG-from-last-tag.md)

> run cli: ./cocochagen -h

```bash
Usage: cocochagen [-hvV] [--[no-]commit-link] [--[no-]issue-link]
               [-c=<releaseCount>] [-F=<template>] [-g=<gitCommitUrl>]
               [-i=<issueUrl>] [--issue-id-pattern=<issueIdRegex>]
               [-n=<releaseName>] [-o=<outputFile>] [-t=<commitType>[,
               <commitType>...]]...
Conventional Commit Changelog Generator

  -c, --release-count=<releaseCount>
                           Last N releases to include into this changelog.
                           Default is '1'
  -F, --template-file=<template>
                           Template file path
                           Used to override the default changelog template. We
                             use Mustache engine.
  -g, --git-commit-url=<gitCommitUrl>
                           Remote url prefix to build commit link (github,
                             gitlab ...)
                           If not provided, we try to read from git remote
                             (origin/master).
  -h, --help               Show this help message and exit.
  -i, --issue-url=<issueUrl>
                           Tracker URL (Jira. github ...)
                           If a card id is found is will be tail at the end
      --issue-id-pattern=<issueIdRegex>
                           a regexp to match an issue id
                           If a card ID is found it will be append at the end
                             of tracker url.
                           Regex must contains 2 named capturing groups:
                              First one named: 'R' is the global one used for
                             link substitution
                              Second one name 'ID' is used to append to issueUrl
                            Example:
                               git: "(?<R>#(?<ID>\\d+))", git conventional
                             commit: (?<R>Closes:[ ]*)#(?<ID>\d+)
                               jira: "(?<R>JIRA-(?<ID>\\d+))"
                           Regex must be java compatible: https://docs.oracle.
                             com/javase/7/docs/api/java/util/regex/Pattern.html
  -n, --release-name=<releaseName>
                           Provide the name of this release
                           By default is automatically computed from last tag if you follow
                             semantic versioning
      --[no-]commit-link   Append git commit URL for change log
                           Default value is 'true'
      --[no-]issue-link    Append an issue link if an issue ID is found into
                             short or full log message
                           Default value is 'true'
  -o, --output=<outputFile>
                           output changelog filename
                           Default value is 'CHANGELOG.md'
  -t, --commit-type=<commitType>[,<commitType>...]
                           Filter only those commits type
                           Default value is 'fix,feat,perf'
  -v, --verbose            print more information on console
                           Default value is 'false'
  -V, --version            Print version information and exit.
```

# Features

- [x] Simple Command line interface.
- [x] Option to generate release note for last N releases.
- [x] Option to filter convention commit.
- [x] Option to provide your own custom template ([mustache](https://mustache.github.io/) based. More info at [samskivert](https://github.com/samskivert/jmustache) ).
- [x] Option to add your own Issuer URL (default is url based on git remote).
- [x] Option to provide your own regex to match Issue ID.
- [x] Option to add/hide: "commit ID", "issues ID", "Contributor".
- [ ] Option to customize Commit Type (Features, ...)
- [ ] Do not override existing changelog. add option to force overriding.
- [ ] Generate changelog at beginning of an existing one.
- [ ] Maven Plugin
- [ ] Gradle Plugin
- [ ] Native CLI with GraalVM

# Reference Documentation

