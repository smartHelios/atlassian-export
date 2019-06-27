package de.smarthelios.confluence.export.model

import de.smarthelios.atlassian.export.model.Image
import de.smarthelios.atlassian.io.Gif
import spock.lang.Specification

class ImageTest extends Specification {


    def 'exportFilename test'() {
        given:
        Image img


        when:
        img = new Image(
                downloadUrl: '/download/attachments/embedded-page/TEST/test/peak.jpeg?api=v2',
                bytes: Gif.GREEN.bytes)
        then:
        img.exportFilename == "peak_${Gif.GREEN.bytes.md5()}.jpeg"


        when:
        img = new Image(
                downloadUrl: '/path/to/pix.gif')
        then:
        img.exportFilename == 'pix.gif'


        when:
        img = new Image(
                downloadUrl: 'relative/path/to/pix.gif',
                bytes: Gif.GREEN.bytes,
                namingHint: 'roadmap'
        )
        then:
        img.exportFilename == 'roadmap_pix.gif'


        when:
        img = new Image(
                downloadUrl: 'pix.gif')
        then:
        img.exportFilename == 'pix.gif'


        when:
        img = new Image(
                downloadUrl: 'no%20fun%20to%32convert.gif',
                bytes: Gif.GREEN.bytes)
        then:
        img.exportFilename == "${Gif.GREEN.md5}.gif"

        when:
        img = new Image(
                downloadUrl: 'no%20fun%20to%32convert.gif',
                bytes: Gif.GREEN.bytes,
                namingHint: 'sample')
        then:
        img.exportFilename == "sample_${Gif.GREEN.md5}.gif"

        when:
        img = new Image(
                downloadUrl: 'no%20fun%20to%32convert.doc',
                namingHint: 'there_are')
        then:
        img.exportFilename == 'there_are_no-bytes.doc'
    }

}
