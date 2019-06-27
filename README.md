# atl-export - Atlassian export tool

A tool for exporting contents from Atlassian Confluence and JIRA.

The exports come as a directory containing Confluence pages or JIRA issues
in HTML format and all attached media like images and attached files. Links
between exported data are local. Links to data not exported will be connected
to the source system. 

## Prerequisites

* Have java installed (e.g. by <https://sdkman.io/>)

## Build

### Distribution

`./gradlew distTar`

Find the resulting distribution package in `build/distributions/atl-export.tar`.

### Local package test

`./gradlew installDist`

Find an unpacked distribution under `build/install/atl-export`.

## Run

`./bin/atl-export`

### Examples

Exporting something from Confluence

```sh
export ATL_EXP_PW=s3cr3t # confluence user password

bin/atl-export -t confluence -u user.name -o /path/to/confluence-out https://confluence.example.org TEST 'test page title'

bin/atl-export -t jira -u user.name -o /path/to/jira-out https://jira.example.org "project = TEST"
```

Getting help

```sh
bin/atl-export -h
```

### Logging

The project uses [SLF4J](https://www.slf4j.org/) and its 
[SimpleLogger](https://www.slf4j.org/apidocs/org/slf4j/impl/SimpleLogger.html).

Set the log level by using Java system property `-Dorg.slf4j.simpleLogger.defaultLogLevel=(debug|info|warn|error)`.
You can define a log output file by setting `-Dorg.slf4j.simpleLogger.logFile=/path/to/logFile`.

Find more options in [SimpleLogger's API documentation](https://www.slf4j.org/apidocs/org/slf4j/impl/SimpleLogger.html).

## Development

### Groovy / Gradle

The project is written in [Groovy](https://groovy-lang.org/) and the build tool is 
[Gradle](https://gradle.org/).

The project directory layout is using the common Java / Groovy conventions based on
[Maven directory layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html).
You will find the sources in `src/main/` and the test sources in `src/test/`. The package structure is divided 
into the three parts `atlassian`, `confluence` and `jira`. Everything in `atlassian` is not JIRA or Confluence spcific code
and often used by both supported export types (`jira` and `confluence`).
To keep the project easy to refactor direct dependencies between packages `jira`
and `confluence` are undesired.

### Docker compose for test instances of Confluence and JIRA

There is a `docker-compose.yml` file in the project root. It starts a JIRA instance
and a Confluence instance. As both need local volumes they are configured
to place them in `./docker/volumes`.

```sh
# start
docker-compose up
# stop
docker-compose down
```

The instances will be reachable under <http://localhost:8090> (Confluence) and <http://localhost:8080> (JIRA).

### Test data

After setting up Confluence you can import a sample Confluence space "TEST" for testing from
`./src/test/resources/confluence/Confluence-space-export-TEST.xml.zip`.
Therefore navigate in Confluence to "Administration" / "General configuration" / "Administration - Backup & Restore"
/ "Import Confluence data" / "Upload a site or space export file" and upload the ZIP file.

## Links

* [JIRA REST API](https://docs.atlassian.com/software/jira/docs/api/REST/latest/#api/2/)
* [Confluence REST API](https://docs.atlassian.com/ConfluenceServer/rest/latest/)
