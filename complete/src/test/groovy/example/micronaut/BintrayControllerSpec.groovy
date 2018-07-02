package example.micronaut

import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxStreamingHttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.reactivex.Flowable
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class BintrayControllerSpec extends Specification {

    @Shared
    @AutoCleanup // <1>
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer) // <2>

    @Shared
    @AutoCleanup
    RxStreamingHttpClient client = embeddedServer.applicationContext.createBean(RxStreamingHttpClient, embeddedServer.getURL()) // <3>

    @Shared
    List<String> expectedProfileNames = ['base', 'federation', 'function', 'function-aws', 'service']

    def "Verify bintray packages can be fetched with low level HttpClient"() {
        when:
        HttpRequest request = HttpRequest.GET('/bintray/packages-lowlevel')

        HttpResponse<List<BintrayPackage>> rsp = client.toBlocking().exchange(request, // <4>
                Argument.of(List, BintrayPackage)) // <5>

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK // <6>
        rsp.body() // <7>

        when:
        List<BintrayPackage> packages = rsp.body()

        then:
        packages*.name.every { expectedProfileNames.contains(it) }
    }

    def "Verify bintray packages can be fetched with compile-time autogenerated @Client"() {
        when:
        HttpRequest request = HttpRequest.GET('/bintray/packages')

        Flowable<BintrayPackage> bintrayPackageStream = client.jsonStream(request, BintrayPackage) // <8>
        Iterable<BintrayPackage> bintrayPackages = bintrayPackageStream.blockingIterable()

        then:
        bintrayPackages*.name.every { expectedProfileNames.contains(it) }
    }
}
