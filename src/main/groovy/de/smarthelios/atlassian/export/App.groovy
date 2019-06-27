package de.smarthelios.atlassian.export

import groovy.cli.commons.CliBuilder
import groovy.util.logging.Slf4j
import de.smarthelios.confluence.export.ConfluenceExport
import de.smarthelios.confluence.io.ConfluenceClient
import de.smarthelios.jira.export.JiraExport
import de.smarthelios.jira.io.JiraClient

/**
 * Main class providing the CLI for exporting Atlassian JIRA and Confluence content
 *
 * see
 * https://stackoverflow.com/questions/53075821/export-access-confluence-page-as-html-including-graphics
 *
 */

@Slf4j
class App {

    static final String COMMAND = 'atl-export'

    static final String HELP = '''\
        NAME
               atl-export - export contents from Atlassian Confluence and JIRA

        SYNOPSIS
               atl-export [-h] [-t exportType] [-u user] [-p password] [-o output dir] [base url] [export type specific options ...]

        DESCRIPTION
               This tool exports contents in an opinionated way from Atlassian Confluence and JIRA.
               Opinionated in a way that you have less options to influence the export.

        OPTIONS
               The -p option should not be used in production as your user password may sneak into shell history or logs,
               Instead use the ATL_EXP_PW environment variable to set the password for accessing the Confluence or JIRA systems. 

               -t exportType
                      controls the type of export. exportType can be "confluence" or "jira" ("jira" is still not supported and under development 

               -u user
                      user name for authentication against Confluence or JIRA. For security reasons we recommend to create a read only user for exports.

               -p password
                      set password testing not recommended for production use. Use ATL_EXP_PW instead.

               -o output dir
                      output directory to store export. Must not exist as atl-export will not export anything to prevent accidental overrides.

               -h shows this help.
               
               -dryRun
                      allows quick testing of export settings by doing only necessary REST API requests and mocking images and attachments data. 

               base url
                      the base url to reach the system to export. e.g. "https://confluence.example.org:8090" 

               export type specific options

                      export type "confluence"
                      a list of space key page options to define root pages for exports
                      e.g. TEST "The test page" INIT "Another page" TEST "Another page in space Test" ...


                      export type "jira"
                      a JQL (JIRA Query Language) query to select issues to export.       

        ENVIRONMENT VARIABLES
               ATL_EXP_PW    password for authentication in production environments.

        EXAMPLES
               atl-export -t confluence -u ann -p anns_pass -o /tmp/doc http://confluence.com:8090 TEST test

               Exports page "test" and all its sub pages of space TEST from confluence host "confluence.com" listening on port 8090 to directory "/tmp/doc".
               Uses user "ann" with password "anns_pass". 
        '''.stripIndent()

    enum ExportType { confluence, jira }

    private String user
    private String password
    private ExportType exportType
    private File outputDir
    private List<String> exportArgs

    private boolean dryRun = false

    static void main(String[] args) {
        new App().run(args)
    }

    void run(String[] args) {

        // atl-export -t confluence -u user -p password -o outDir http://confluence.de:8090
        def cli = new CliBuilder(usage:COMMAND)
        cli.t(type:ExportType, 'Export type is one of confluence|jira (currently only "confluence" is supported).')
        cli.u(type:String, 'User name for authentication.')
        cli.p(type:String, 'Password for authentication. Do not use in production! Use ATL_EXP_PW environment variable instead.')
        cli.o(type:File, 'Output directory. Must not exist. Will be created by export.')
        cli.dryRun(type:Boolean, 'Fast export testing by not downloading attachments and images but replacing them with mock data.')
        cli.h('Shows help.')

        def options = cli.parse(args)

        if(options) {
            if(options.h) {
                System.out.println HELP
            }
            else if(options.t && options.u && options.o && options.arguments().size() > 0) {
                dryRun = options.dryRun ?: false

                exportType = options.t
                user = options.u

                if(options.p) {
                    password = options.p
                }
                else {
                    password = System.getenv('ATL_EXP_PW')
                }

                outputDir = options.o

                exportArgs = options.arguments()

                switch (exportType) {
                    case ExportType.confluence:
                        exportConfluencePages()
                        break
                    case ExportType.jira:
                        exportJiraIssues()
                        break
                    default:
                        log.error 'Export type not supported / under development: {}', exportType
                }
            }
            else {
                cli.usage()
            }

        }
        else {
            cli.usage()
        }


    }

    private void exportConfluencePages() {
        if(exportArgs.size() < 3) {
            log.error('Not at least baseUrl and one spaceKey pageTitle given.')
        }
        else {
            log.debug 'exportConfluencePages: parsing arguments'

            String baseUrl = exportArgs.first()
            URL url = new URL(baseUrl)

            String scheme = url.protocol
            int port = url.port ?: url.defaultPort
            String host = url.host

            String[] spaceKeyPageTuples = exportArgs.tail()



            log.info 'Confluence page export starts.'

            ConfluenceClient confluenceClient = new ConfluenceClient(
                    host,
                    user,
                    password,
                    port,
                    scheme
            )

            ConfluenceExport export = new ConfluenceExport(confluenceClient)

            boolean result
            try {
                result = export.export(spaceKeyPageTuples, outputDir)
            }
            finally {
                confluenceClient.close()
            }

            if(result) {
                log.info 'Confluence page export finished.'
            }
            else {
                log.info 'Confluence page export finished WITH ERRORS.'
            }
        }
    }

    private void exportJiraIssues() {
        if(exportArgs.size() < 2) {
            log.error('Not at least baseUrl and jql given.')
        }
        else {

            String baseUrl = exportArgs.first()
            URL url = new URL(baseUrl)

            String scheme = url.protocol
            int port = url.port ?: url.defaultPort
            String host = url.host

            String jql = exportArgs.tail().first()

            log.info('Jira issues export starts.')

            JiraClient jiraClient = new JiraClient(
                    host,
                    user,
                    password,
                    port,
                    scheme
            )
            jiraClient.dryRun = dryRun

            JiraExport export = new JiraExport(jiraClient)

            boolean result
            try {
                result = export.export(jql, outputDir)
            }
            finally {
                jiraClient.close()
            }

            if(result) {
                log.info 'Jira issues export finished.'
            }
            else {
                log.info 'Jira issues export finished WITH ERRORS.'
            }

        }
    }

}
