package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/4/14.
 */
class KeystoneV3AuthZTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockKeystoneV3Service fakeKeystoneV3Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/common", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeKeystoneV3Service.handler)
    }


    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()
    }


    def "When user is authorized should forward request to origin service"(){
        given:
        fakeKeystoneV3Service.with {
            endpointUrl = endpointResponse
        }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint + "/v3/"+fakeKeystoneV3Service.client_token+"/ss", method:'GET', headers:['X-Subject-Token': fakeKeystoneV3Service.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        where:
        endpointResponse | statusCode
        "localhost"      | "200"
        "invalid"        | "403"

    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                        "x-pp-user": "usertest1, usertest2, usertest3",
                        "accept": "application/xml;q=1 , application/json;q=0.5",
                        'X-Subject-Token': fakeKeystoneV3Service.client_token
                ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/v3/"+fakeKeystoneV3Service.client_token+"/ss",
                method: 'GET',
                headers: reqHeaders)

        then: "User should receive a 200 response"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 4
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }
        Map<String, String> headers = ['X-Subject-Token': fakeKeystoneV3Service.client_token]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/"+fakeKeystoneV3Service.client_token+"/ss",
                method: 'GET', headers: headers, defaultHandler: handler)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "201"
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'X-Subject-Token': fakeKeystoneV3Service.client_token
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/v3/"+fakeKeystoneV3Service.client_token+"/ss",
                method: 'GET',
                headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
    }

    @Unroll("Responses - headers: #headerName with \"#headerValue\" keep its case")
    def "Responses - header keep its case in responses"() {
        given:
        def headers = ['X-Subject-Token': fakeKeystoneV3Service.client_token]
        when: "make a request with the given header and value"
        def respHeaders = [
                "location": "http://somehost.com/blah?a=b,c,d"
        ]
        respHeaders[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v3/"+fakeKeystoneV3Service.client_token+"/ss",
                method: 'GET', headers: headers, defaultHandler: { new Response(201, "Created", respHeaders, "") })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName | headerValue
        "Content-Type" | "application/json"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
    }

    def "When user is not authorized should receive a 403 FORBIDDEN response"(){

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeKeystoneV3Service.client_token = token

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint + "/v1/"+token+"/ss", method:'GET', headers:['X-Subject-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in keystone-v3.cfg.xml")

        then: "User should receive a 403 FORBIDDEN response"
        foundLogs.size() == 1
        mc.handlings.size() == 0
        mc.receivedResponse.code == "403"
    }

}
