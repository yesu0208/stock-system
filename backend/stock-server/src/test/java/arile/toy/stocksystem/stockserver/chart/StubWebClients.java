package arile.toy.stocksystem.stockserver.chart;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** 실제 HTTP 없이 요청별 JSON 응답을 돌려주는 테스트용 WebClient */
public final class StubWebClients {

    private StubWebClients() {
    }

    public static WebClient of(List<ClientRequest> captured, Function<ClientRequest, String> responder) {
        return WebClient.builder()
                .baseUrl("http://chart")
                .exchangeFunction(request -> {
                    captured.add(request);
                    String body = responder.apply(request);
                    ClientResponse.Builder response = ClientResponse.create(HttpStatus.OK);
                    // null이면 본문·Content-Type 없는 응답 (bodyToMono가 비어 block()이 null을 돌려줌)
                    if (body != null) {
                        response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(body);
                    }
                    return Mono.just(response.build());
                })
                .build();
    }

    public static WebClient of(Function<ClientRequest, String> responder) {
        return of(new ArrayList<>(), responder);
    }

    public static String query(ClientRequest request, String name) {
        return org.springframework.web.util.UriComponentsBuilder.fromUri(request.url()).build()
                .getQueryParams().getFirst(name);
    }
}
