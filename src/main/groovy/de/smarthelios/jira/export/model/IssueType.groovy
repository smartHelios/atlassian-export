package de.smarthelios.jira.export.model

import de.smarthelios.atlassian.export.model.Image

/**
 * Models the type of an issue like "Sub-task", "Epic", "Story", "Task" or "Bug".
 */
class IssueType {

    String id
    String name
    String description
    boolean subtask
    String iconUrl
    String avatarId

    Image iconImage


    // Using this boolean properties is preferred, as it is not sure that name and/or ids of issue types are really fixed

    boolean isTask() {
        name == 'Task'
    }

    boolean isSubTask() {
        name == 'Sub-task' || subtask
    }

    boolean isStory() {
        name == 'Story'
    }

    boolean isBug() {
        name == 'Bug'
    }

    boolean isEpic() {
        name == 'Epic'
    }

    boolean isSpike() {
        name == 'Spike'
    }

    boolean isImprovement() {
        name == 'Improvement'
    }
}
