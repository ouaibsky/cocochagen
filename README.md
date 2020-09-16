- [Read Me First.](#read-me-first)
- [Getting Started.](#getting-started)
    + [Download an executable jar.](#download-an-executable-jar)
    + [Maven: add to your build.](#maven--add-to-your-build)
    + [Gradle: add to your build.](#gradle--add-to-your-build)
    + [CLI](#cli)
    + [Usage](#usage)
- [Features](#features)
- [Trouble Shooting](#trouble-shooting)

# Read Me First.

* [Convention Commit](https://www.conventionalcommits.org/) Changelog Generator (obviously for Git)

# Getting Started.

* [look at output example](examples/CHANGELOG-from-last-tag.md)

### Download an executable jar.

* last version

    * [cococha](./cococha.jar)

* Maven (all versions) 

    * Download the executable jar from maven central (cocochagen-cli-x.y.z.jar).
Can be renamed whatever.

        ```xml
        <dependency>
          <groupId>org.icroco.cocochagen</groupId>
          <artifactId>cocochagen-cli</artifactId>
          <version>1.0.1</version>
        </dependency>
        ```

### Maven: add to your build.

```xml
      <plugin>
          <groupId>org.icroco.cocochagen</groupId>
          <artifactId>cocochagen-maven-plugin</artifactId>
          <version>1.0.4</version>
          <configuration>
              <overrideExisting>true</overrideExisting>
              <releaseCount>5</releaseCount>
              <filterCommitTypes>*</filterCommitTypes>
          </configuration>
      </plugin>
```

* Use maven help to list all parameters:
> mvn help:describe -DgroupId=org.icroco.cocochagen -DartifactId=cocochagen-maven-plugin  -Ddetail

* to ask for a giver version:
> mvn help:describe -DgroupId=org.icroco.cocochagen -DartifactId=cocochagen-maven-plugin -Dversion=1.0.4 -Ddetail

* Have a look to [this example](./maven-plugin/examples/help/pom.xml) for all configuration properties.

### Gradle: add to your build.

TODO
    
### CLI
#### Linux /Mac
> ./cococha -h

or

> java -jar cococha.jar -h

#### Window

> java -jar cococha.jar -h

### Usage

```bash
Usage: cocochagen [-hvV] [--[no-]commit-link] [--[no-]issue-link] [-c=<releaseCount>] [-F=<template>] [-g=<gitCommitUrl>] [-i=<issueUrl>]
                  [--issue-id-pattern=<issueIdRegex>] [-n=<releaseName>] [-o=<outputFile>] [-t=<commitType>[,<commitType>...]]...
                  ([--output-override] | [--output-append-start])
Conventional Commit Changelog Generator

  -c, --release-count=<releaseCount>
                           Last N releases to include into this changelog.
                           Default is: '1'

  -F, --template-file=<template>
                           Template file path
                           Used to override the default changelog template. We use Mustache engine.
                           Option undefined means we'll pick up the embedded one

  -g, --git-commit-url=<gitCommitUrl>
                           Remote url prefix to build commit link (github, gitlab ...)
                           Option undefined means we'll try to read from git remote (origin/master).

  -h, --help               Show this help message and exit.
  -i, --issue-url=<issueUrl>
                           Tracker URL (Jira. github ...)
                           If a card/issue ID is found is will be tail at the end
                           Option undefined means we'll used github

      --issue-id-pattern=<issueIdRegex>
                           a regexp to match an issue id
                           If a card ID is found it will be append at the end of tracker url.
                           Regex must contains 1 global group and 1 named capturing groups:
                              A global one used to identify an entirely issue id (ex: Closes: #1234)
                              Second one is named 'ID', used to extract the id that will be appended after issueUrl (ex: 1234)
                            Examples:
                               git: "(#(?<ID>\\d+))",
                               jira: "(?<ID>JIRA-\\d+)"
                               Strict conventional commit: "(Closes: )#(?<ID>\\d+)"
                               Advanced conventional commit: "(([Cc][Ll][Oo][Ss][Ee][Ss][ \t]*:[ \t]*)?#(?<ID>\\d+))"
                               Mix git and coco style: "(([Cc][Ll][Oo][Ss][Ee][Ss][ \t]*:[ \t]*)?#?(?<ID>\\d+))"
                           Regex must be java compatible: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
                           Default is git style optionally with prefix 'Closes: ' '(([Cc][Ll][Oo][Ss][Ee][Ss][ 	]*:[ 	]*)?#(?<ID>\d+))'

  -n, --release-name=<releaseName>
                           Provide the name of this release
                           By default is automatically computed from last tag if you follow semantic versioning
                           Option undefined means automatic release name'

      --[no-]commit-link   Add git commit URL for change log
                           Default is: 'true'

      --[no-]issue-link    Append an issue link if an issue ID is found into short or full log message
                           Default is: 'true'

  -o, --output=<outputFile>
                           output changelog filename

      --output-append-start
                           Append the output on top of an existing file
                           Default is: 'false'

      --output-override    Override if output already exists
                           Default is: 'false'

  -t, --commit-type=<commitType>[,<commitType>...]
                           Filter only those commits type
                           Default is: 'fix,feat,perf'

  -v, --verbose            print more information on console
                           Default is: 'false'

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
- [ ] Option to add/hide: "Contributor".
- [ ] Option to customize Commit Type label (Features, ...)
- [x] Merge similar commits description and support many CommitID links per line.
- [x] Do not override existing changelog. add option to force overriding.
- [x] Generate changelog at beginning of an existing one.
- [ ] Export report into json format
- [x] Publish to maven central
- [ ] Add Travis
- [x] Maven Plugin
- [ ] Gradle Plugin
- [ ] Native CLI with GraalVM

# Trouble Shooting
* Runtime working dir: be careful, right now you won't have same result if your run with **java -jar ...** ot **./cocochgen.jar**
    * with java -jar working dir is the current dir where you launch the command
    * running like an executable **./cococha.jar**, the working dir will be where jar is located
    > example: './foo/bar/cocochagen.jar' is executing into the directory './foo/bar'
* If your changelog looks not as expected (missing items,  ...), try to run with these options:
    > ./cococha.jar -t '*' -c 10
                                                                                                   >
* Finally, use **-v** to get more details on stdout. 


<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

