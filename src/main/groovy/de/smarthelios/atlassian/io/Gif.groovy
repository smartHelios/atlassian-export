package de.smarthelios.atlassian.io

/**
 * An enumeration of potential useful image data for replacements in Atlassian generated HTML.
 */
enum Gif {

    // single pixel GIFs
    TRANSPARENT('transparent_1x1.gif','R0lGODlhAQABAIAAAP///////yH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=='),
    RED('red_1x1.gif','R0lGODdhAQABAIABAP8AAP///ywAAAAAAQABAAACAkQBADs='),
    GREEN('green_1x1.gif','R0lGODdhAQABAIABAAD/AP///ywAAAAAAQABAAACAkQBADs='),
    BLUE('blue_1x1.gif','R0lGODdhAQABAIABAAAA/////ywAAAAAAQABAAACAkQBADs='),
    CYAN('cyan_1x1.gif','R0lGODdhAQABAIABAAD//////ywAAAAAAQABAAACAkQBADs='),
    MAGENTA('magenta_1x1.gif','R0lGODdhAQABAIABAP8A/////ywAAAAAAQABAAACAkQBADs='),
    YELLOW('yellow_1x1.gif','R0lGODdhAQABAIABAP//AP///ywAAAAAAQABAAACAkQBADs='),
    BLACK('black_1x1.gif','R0lGODdhAQABAIABAAAAAP///ywAAAAAAQABAAACAkQBADs='),
    WHITE('white_1x1.gif','R0lGODdhAQABAIAAAP///////ywAAAAAAQABAAACAkQBADs='),
    RED_WHITE('red_white_2x2.gif','R0lGODdhAgACAIABAP8AAP///ywAAAAAAgACAAACA0QCBQA7'),
    // small "DRY RUN" GIF
    TEXT_DRY_RUN('dry_run_16x16.gif','R0lGODdhEAAQAIAAAP8AAP///ywAAAAAEAAQAAACK4yPacDbAZaElMZ6Y3ZnszthVod8k6ak6pqa4vZKDYyOj+tBuT6KGM1KFQAAOw==')

    final String filename
    final byte[] bytes

    Gif(String filename, String bytesBase64) {
        this.filename = filename
        this.bytes = bytesBase64.decodeBase64()
    }

    String getBase64() {
        bytes.encodeBase64()
    }

    String getMd5() {
        bytes.md5()
    }
}