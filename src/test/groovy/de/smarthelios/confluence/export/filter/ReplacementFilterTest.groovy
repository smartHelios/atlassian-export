package de.smarthelios.confluence.export.filter


import de.smarthelios.confluence.export.ConfluenceExport
import de.smarthelios.confluence.export.model.Page
import spock.lang.Specification

class ReplacementFilterTest extends Specification {
    def 'Replaces confluence page URLs with local page urls'() {

        given:
        def page = new Page(
                id:'12345',
                title:'Test',
                exportView: '''\
                    <div>
                        <a href="http://localhost:8090/pages/viewpage.action?pageId=67890">foo</a>
                    </div>
                    <div>
                        <a href="http://localhost:8090/pages/viewpage.action?pageId=78901">bar</a>
                    </div>
                    <div>
                        <a href="http://example.org">example</a>
                    </div>
                    '''.stripIndent()
        )
        def otherPages = [
                new Page(id: '67890',title:'Exportable Title'),
                new Page(id: '78901',title: 'Bad Title ?'),
                page
        ]
        def filter = ConfluenceExport.buildHrefFilter(otherPages, 'http://localhost:8090')

        when:
        def out = page.exportViewFiltered([filter])

        then:
        out == '''\
                <div>
                    <a href="Exportable_Title_67890.html">foo</a>
                </div>
                <div>
                    <a href="78901.html">bar</a>
                </div>
                <div>
                    <a href="http://example.org">example</a>
                </div>
                '''.stripIndent()
    }

}
