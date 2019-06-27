package de.smarthelios.atlassian.io

import spock.lang.Specification

class MimeTypeFileExtTest extends Specification {
    def 'decides if file and mimetype matches'() {

        given:
        def m = MimeTypeFileExt.instance

        expect:

        m.matches('image/png', 'foo.PNG')
        m.matches('image/png', 'foo.png')
        !m.matches('image/png', 'foo.PnG')

        m.matches('image/jpeg', 'foo.jpeg')
        m.matches('image/jpeg', 'foo.jpg')
        m.matches('image/jpeg', 'foo.jpe')
        m.matches('image/jpeg', 'foo.JPEG')
        m.matches('image/jpeg', 'foo.JPG')
        m.matches('image/jpeg', 'foo.JPE')
        !m.matches('image/jpeg', 'foo.Jpeg')
        !m.matches('image/jpeg', 'foo.jPg')
        !m.matches('image/jpeg', 'foo.jpE')

        !m.matches('not/a/mimeType','foo.jpg')
    }

    def 'returns the best file extension for mime type'() {
        given:
        def m = MimeTypeFileExt.instance

        expect:
        m.extFor('image/jpeg') == 'jpg'
        m.extFor('application/xhtml+xml') == 'xht'
        m.extFor('application/vnd.solent.sdkm+xml') == 'sdkm'
        m.extFor('application/vnd.visio') == 'vsd'
        m.extFor('not/a/mimeType') == ''
    }

    def 'returns matching extension in the letter case of the filename'() {
        given:
        def m = MimeTypeFileExt.instance

        expect:
        m.matchingExt('foo.jpeg') == 'jpeg'
        m.matchingExt('foo.jpg')  == 'jpg'
        m.matchingExt('foo.jpe')  == 'jpe'
        m.matchingExt('foo.JPEG') == 'JPEG'
        m.matchingExt('foo.JPG')  == 'JPG'
        m.matchingExt('foo.JPE')  == 'JPE'
        m.matchingExt('foo.Jpeg') == ''
        m.matchingExt('foo.jPg')  == ''
        m.matchingExt('foo.jpE')  == ''
    }

    def 'matchingExt() is null safe'() {
        given:
        def m = MimeTypeFileExt.instance

        expect:
        m.matchingExt(null) == ''
    }

    def 'extFor() is null safe'() {
        given:
        def m = MimeTypeFileExt.instance

        expect:
        m.extFor(null) == ''
    }

    def 'matches() is null safe'() {
        given:
        def m = MimeTypeFileExt.instance

        expect:
        !m.matches(null, null)
        !m.matches(null, 'foo.jpeg')
        !m.matches('image/jpeg', null)
    }

    def 'extFor() ignores encoding suffix'() {
        given:
        def m = MimeTypeFileExt.instance

        expect:
        m.extFor('image/svg+xml;charset=UTF-8') == 'svg'
    }
}
