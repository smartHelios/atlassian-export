package de.smarthelios.atlassian.io


import de.smarthelios.test.WireMockSpecification

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get

class HttpClientTest extends WireMockSpecification {

    def 'makes simple http request'() {
        given:
        wireMockServer.stubFor(get('/ok').willReturn(
                aResponse().withBody('OK')
        ))
        HttpClient client = new HttpClient('localhost', wireMockServer.port(), 'http')

        when:
        String indexHtml = client.doGet('/ok')

        then:
        indexHtml == 'OK'
    }

    def 'makes simple binary http request'() {
        given:
        wireMockServer.stubFor(get('/green_pixel.gif').willReturn(
                aResponse().withBody(Gif.GREEN.bytes)
        ))
        HttpClient client = new HttpClient('localhost', wireMockServer.port(), 'http')

        when:
        String bytesBase64 = client.doGetBytes('/green_pixel.gif').bytes.encodeBase64()

        then:
        bytesBase64 == Gif.GREEN.base64
    }
}
