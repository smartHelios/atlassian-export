package de.smarthelios.test

import com.github.tomakehurst.wiremock.WireMockServer
import spock.lang.Specification

class WireMockSpecification extends Specification {

    protected WireMockServer wireMockServer
    protected String baseUrl

    void setup() {
        wireMockServer = new WireMockServer(0, 0)
        wireMockServer.start()

        baseUrl = "http://localhost:${wireMockServer.port()}"
    }

    void cleanup() {
        baseUrl = null

        wireMockServer?.stop()
        wireMockServer = null
    }

}
