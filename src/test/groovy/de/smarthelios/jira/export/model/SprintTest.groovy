package de.smarthelios.jira.export.model

import spock.lang.Specification

import java.time.temporal.ChronoField

class SprintTest extends Specification {
    def 'parsesUglySerialization'() {
        given:
        Sprint sprint

        when:
        sprint = Sprint.fromUglySerialization('com.atlassian.greenhopper.service.sprint.Sprint@53e7888[id=83,rapidViewId=61,state=CLOSED,name=Sprint 7 - Cook Patients,startDate=2019-05-20T16:05:33.135+02:00,endDate=2019-05-31T16:05:00.000+02:00,completeDate=2019-06-03T14:09:45.110+02:00,sequence=83,goal=- Responsive Admin UI- Audio unit for patient app]')
        then:
        sprint.id == '83'
        sprint.rapidViewId == '61'
        sprint.state == 'CLOSED'
        sprint.name == 'Sprint 7 - Cook Patients'
        sprint.goal == '- Responsive Admin UI- Audio unit for patient app'
        sprint.startDate.toLocalDateTime().get(ChronoField.YEAR) == 2019
        sprint.startDate.toLocalDateTime().get(ChronoField.MONTH_OF_YEAR) == 5
        sprint.startDate.toLocalDateTime().get(ChronoField.SECOND_OF_MINUTE) == 33


        when:
        sprint = Sprint.fromUglySerialization('com.atlassian.greenhopper.service.sprint.Sprint@14b88eac[id=84,rapidViewId=61,state=ACTIVE,name=Sprint 8 - PROM Bug Bunny,startDate=2019-06-03T14:46:13.309+02:00,endDate=2019-06-14T14:46:00.000+02:00,completeDate=<null>,sequence=84,goal=Enable patient creation/search for doctors and therapists as well as improve PROMs]')
        then:
        sprint.id == '84'
        sprint.name == 'Sprint 8 - PROM Bug Bunny'
        sprint.completeDate == null
    }
}
