# Read Me First
[Convention Commit](https://www.conventionalcommits.org/) Changelog Generator

# Getting Started

[look at output example](examples/CHANGELOG-from-last-tag.md)

> run cli: ./cocochagen -h

```bash
Usage: cocochagen [-hvV] [--[no-]commit-link] [--[no-]issue-link] [-c=<releaseCount>] [-F=<template>] [-g=<gitCommitUrl>] [-i=<issueUrl>]
                  [--issue-id-pattern=<issueIdRegex>] [-n=<releaseName>] [-o=<outputFile>] [-t=<commitType>[,<commitType>...]]...
Conventional Commit Changelog Generator

  -c, --release-count=<releaseCount>
                           Last N releases to include into this changelog.
                           Default is: '1'

  -F, --template-file=<template>
                           Template file path
                           Used to override the default changelog template. We use Mustache engine.
                           Option not defined means we'll pick up the one embedded

  -g, --git-commit-url=<gitCommitUrl>
                           Remote url prefix to build commit link (github, gitlab ...)
                           Option not defined means we'll try to read from git remote (origin/master).

  -h, --help               Show this help message and exit.
  -i, --issue-url=<issueUrl>
                           Tracker URL (Jira. github ...)
                           If a card id is found is will be tail at the end
                           Option not defined means we'll used github style'

      --issue-id-pattern=<issueIdRegex>
                           a regexp to match an issue id
                           If a card ID is found it will be append at the end of tracker url.
                           Regex must contains 2 named capturing groups:
                              First one named: 'R' is the global one used for link substitution
                              Second one name 'ID' is used to append to issueUrl
                            Example:
                               git: "(?<R>#(?<ID>\\d+))", git conventional commit: (?<R>Closes:[ ]*)#(?<ID>\d+)
                               jira: "(?<R>JIRA-(?<ID>\\d+))"
                           Regex must be java compatible: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
                           Default is '(?<R>([Cc][Ll][Oo][Ss][Ee][Ss][ 	]*:[ 	]*)?#(?<ID>\d+))'

  -n, --release-name=<releaseName>
                           Provide the name of this release
                           By default is automatically computed from last tag if you follow semantic versioning
                           Option not defined means automatic release name'

      --[no-]commit-link   Add git commit URL for change log
                           Default is: 'true'

      --[no-]issue-link    Append an issue link if an issue ID is found into short or full log message
                           Default is: 'true'

  -o, --output=<outputFile>
                           output changelog filename

  -t, --commit-type=<commitType>[,<commitType>...]
                           Filter only those commits type
                           Default is: 'fix,feat,perf'

  -v, --verbose            print more information on console
                           Default is: 'false'

  -V, --version            Print version information and exit.
````

# Features

- [x] Simple Command line interface.
- [x] Option to generate release note for last N releases.
- [x] Option to filter convention commit.
- [x] Option to provide your own custom template ([mustache](https://mustache.github.io/) based. More info at [samskivert](https://github.com/samskivert/jmustache) ).
- [x] Option to add your own Issuer URL (default is url based on git remote).
- [x] Option to provide your own regex to match Issue ID.
- [x] Option to add/hide: "commit ID", "issues ID", "Contributor".
- [ ] Option to add/hide: "Contributor".
- [ ] Option to customize Commit Type label (Features, ...)
- [x] Do not override existing changelog. add option to force overriding.
- [x] Generate changelog at beginning of an existing one.
- [ ] Maven Plugin
- [ ] Gradle Plugin
- [ ] Native CLI with GraalVM

# Trouble Shooting
* Runtime working dir: be careful, right now you won't have same result if your run with **java -jar ...** ot **./cocochgen.jar**
    * with java -jar working dir is the current dir where you launch the command
    * running like an executable **./cocochagen.jar**, the working dir will be where jar is located
    > example: './foo/bar/cocochagen.jar' is executing into directory './foo/bar'
* If your changelog looks not as expected (missing items,  ...), try to run with theses options:
    > ./cocochagen -t '*' -c 10
                                                                                                   >
* Finally, use **-v** to get more details on stdout. 