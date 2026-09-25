package arile.toy.stocksystem.stockserver.chart.token;

import arile.toy.stocksystem.stockserver.chart.StubWebClients;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("[Token] 차트 API 접근 토큰 관리 테스트")
class ChartApiTokenManagerTest {

    @DisplayName("발급받은 토큰을 보관하고, 응답에 토큰이 없으면 기존 값을 유지한다")
    @Test
    void whenRefreshing_thenStoresTokenOrKeeps() {
        List<ClientRequest> requests = new ArrayList<>();
        List<String> responses = new ArrayList<>(List.of("{\"access_token\":\"t1\"}", "{}"));
        var sut = new ChartApiTokenManager(StubWebClients.of(requests, r -> responses.remove(0)));
        ReflectionTestUtils.setField(sut, "appKey", "key");
        ReflectionTestUtils.setField(sut, "appSecret", "secret");

        sut.init();
        assertThat(sut.getAccessToken()).isEqualTo("t1");
        assertThat(requests.get(0).url().getPath()).isEqualTo("/oauth2/tokenP");

        sut.scheduledRefresh();
        assertThat(sut.getAccessToken()).isEqualTo("t1");
    }

    @DisplayName("발급 중 오류가 나도 예외를 던지지 않는다 (기동 실패 방지)")
    @Test
    void givenError_whenRefreshing_thenDoesNotThrow() {
        var sut = new ChartApiTokenManager(StubWebClients.of(r -> { throw new IllegalStateException("down"); }));

        assertThatNoException().isThrownBy(sut::init);
        assertThat(sut.getAccessToken()).isNull();
    }
}
