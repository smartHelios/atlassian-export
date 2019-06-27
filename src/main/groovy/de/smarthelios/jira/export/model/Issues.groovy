package de.smarthelios.jira.export.model

import groovy.util.logging.Slf4j

import static IssueRelation.Kind

/**
 * Decorator to ease the access to a list of JIRA issues.
 */
@Slf4j
class Issues {
    List<Issue> issues

    List<String> getMissingEpics() {
        epicKeys - keyIndex.keySet()
    }

    Map<String, Issue> getKeyIndex() {
        issues.collectEntries {
            [it.key, it]
        }
    }

    boolean contains(String key) {
        null != issues.find { it.key == key }
    }

    List<String> getEpicKeys() {
        issues.collect { it.epicKey }.findAll { !(null == it || it.empty) }.unique()
    }

    List<Issue> select(List<String> issueKeys) {
        issueKeys.collect { keyIndex[it] }
    }

    List<Issue> getSubTasks() {
        issues.findAll { it.issueType.isSubTask() }
    }

    List<String> getLabels() {
        List<String> labels = issues.collect { it.labels }.flatten() as List<String>
        labels.unique().sort { String it -> it.toLowerCase() }
    }

    List<Issue> issuesForLabel(String label) {
        issues.findAll { label in it.labels }.sort { it.id }
    }

    List<Issue> getWithoutLabels() {
        issues.findAll { !(it.labels) }.sort { it.id }
    }

    /**
     * Analyzes issues list and return a list of relations between the issues.
     * The relations are easier to query for further processing.
     *
     * Issue link relations are not build by default as often linked issues are not fetched and would produce warnings.
     *
     * Resulting relations are structured top down:
     * <ul>
     *     <li>epics to issues with epic link</li>
     *     <li>issues to sub tasks</li>
     *     <li>issue link from inward to outward issue</li>
     * </ul>
     *
     * @param kinds what kind of relations should be build
     * @return a list of relations
     */
    List<IssueRelation> getRelations(Kind... kinds = [Kind.EPIC, Kind.SUB_TASK]) {
        List<IssueRelation> relations = []

        Map<String, Issue> index = keyIndex

        boolean collectEpics = kinds.contains(Kind.EPIC)
        boolean collectSubTasks = kinds.contains(Kind.SUB_TASK)
        boolean collectIssueLinks = kinds.contains(Kind.ISSUE_LINK)

        issues.each { issue ->

            if(collectEpics && issue.epicKey) {
                Issue epic = index[issue.epicKey]
                if(epic) {
                    relations << new IssueRelation(epic, Kind.EPIC, issue)
                }
                else {
                    log.warn 'Could not build epic relation for issue {}. Epic {} is missing in issues list.', issue.key, issue.epicKey
                }
            }

            if(collectSubTasks && issue.subTasks) {
                issue.subTasks.each { subTaskIssue ->
                    Issue subTask = index[subTaskIssue.key]
                    if(subTask) {
                        relations << new IssueRelation(issue, Kind.SUB_TASK, subTask)
                    }
                    else {
                        log.warn 'Could not build sub task relation for issue {}. Sub task {} is missing in issues list.', issue.key, subTaskIssue.key
                    }
                }
            }

            if(collectIssueLinks && issue.issueLinks) {
                issue.issueLinks.each { issueLink ->
                    if(issueLink.inwardIssueKey) {
                        Issue linkIssue = index[issueLink.inwardIssueKey]
                        if(linkIssue) {
                            relations << new IssueRelation(linkIssue, Kind.ISSUE_LINK, issue, issueLink)
                        }
                        else {
                            log.warn 'Could not build link relation for issue {}. Linked inward issue {} is missing in issues list.', issue.key, issueLink.inwardIssueKey
                        }
                    }
                    else if(issueLink.outwardIssueKey) {
                        Issue linkIssue = index[issueLink.outwardIssueKey]
                        if(linkIssue) {
                            relations << new IssueRelation(issue, Kind.ISSUE_LINK, linkIssue, issueLink)
                        }
                        else {
                            log.warn 'Could not build link relation for issue {}. Linked outward issue {} is missing in issues list.', issue.key, issueLink.inwardIssueKey
                        }
                    }
                    else {
                        log.error 'Issue links with neither inward nor outward issues can not be processed. Offending issue link id={} for issue {}.', issueLink.id, issue.key
                    }
                }
            }

        }

        relations
    }
}
