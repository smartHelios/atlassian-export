package de.smarthelios.confluence.export.model

import spock.lang.Specification

class AttachmentTest extends Specification {
    def 'good filename title is extended with id'() {
        given:
        def attachment = new Attachment(
                id: '123',
                title:  'good.jpeg'
        )
        expect:
        attachment.exportFileName == 'good_123.jpeg'
    }

    def 'bad file extension is compensated by mime type'() {
        given:
        def attachment = new Attachment(
                id: '123',
                mediaType: 'image/png',
                title:  'no_Extension_but g00d.Title'
        )
        expect:
        attachment.exportFileName == 'no_Extension_but_g00d_Title_123.png'
    }

    def 'bad file extension and bad mime type cannot be compensated'() {
        given:
        def attachment = new Attachment(
                id: '123',
                mediaType: 'unknown/mime/type',
                title:  'bad_Title'
        )
        expect:
        attachment.exportFileName == 'bad_Title_123'
    }

    def 'if everything is bad just the id stays'() {
        given:
        def attachment = new Attachment(id: '123')
        expect:
        attachment.exportFileName == '123'
    }

    def 'crazy title are not sanitized'() {
        given:
        def attachment = new Attachment(
                id: '123',
                title: '#WTF?$.docx'
        )
        expect:
        attachment.exportFileName == '123.docx'
    }
}
