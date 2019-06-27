package de.smarthelios.jira.export.model

import static de.smarthelios.atlassian.export.convert.Convert.toDate

/**
 * Model representing sprint information attached to an issue.
 */
class Sprint {

    String id
    String name
    String state
    Date startDate
    Date endDate
    Date completeDate
    String rapidViewId
    String goal

    // see https://community.atlassian.com/t5/Jira-questions/Sprint-field-value-REST-API/qaq-p/229495
    static Sprint fromUglySerialization(String serialized) {
        String data = serialized.substring(
                serialized.indexOf('[') + 1,
                serialized.lastIndexOf(']')
        )
        def sprint = data.split(',').collectEntries { it.split('=') }

        new Sprint(
                id: sprint.id,
                name: sprint.name,
                state: sprint.state,
                rapidViewId: sprint.rapidViewId,
                startDate: toDate(sprint.startDate),
                endDate: toDate(sprint.endDate),
                completeDate: toDate(sprint.completeDate),
                goal: sprint.goal
        )
    }

}
