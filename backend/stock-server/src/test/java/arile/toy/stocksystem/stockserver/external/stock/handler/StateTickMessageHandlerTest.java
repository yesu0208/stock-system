package arile.toy.stocksystem.stockserver.external.stock.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("[Handler] 외부 웹소켓 상태 메시지 핸들러 테스트")
class StateTickMessageHandlerTest {

    private final StateTickMessageHandler sut = new StateTickMessageHandler(new ObjectMapper());

    @DisplayName("구독 결과 메시지는, 종류와 관계없이 예외 없이 처리한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "SUBSCRIBE SUCCESS",
            "UNSUBSCRIBE SUCCESS",
            "UNSUBSCRIBE ERROR(not found!)",
            "ALREADY IN SUBSCRIBE",
            "UNKNOWN"
    })
    void givenMsg1_whenHandling_thenLogs(String msg1) {
        String message = "{\"body\":{\"msg1\":\"" + msg1 + "\"}}";

        assertThatNoException().isThrownBy(() -> sut.handle(message));
    }

    @DisplayName("PINGPONG 메시지는, 예외 없이 처리한다.")
    @ParameterizedTest
    @ValueSource(strings = "{\"header\":{\"tr_id\":\"PINGPONG\",\"datetime\":\"20260926113800\"}}")
    void givenPingpong_whenHandling_thenLogs(String message) {
        assertThatNoException().isThrownBy(() -> sut.handle(message));
    }

    @DisplayName("msg1이 없고 PINGPONG도 아닌 메시지는, 아무 처리 없이 넘어간다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"header\":{\"tr_id\":\"OTHER\"}}",
            "{}"
    })
    void givenNeitherMsg1NorPingpong_whenHandling_thenIgnores(String message) {
        assertThatNoException().isThrownBy(() -> sut.handle(message));
    }

    @DisplayName("JSON이 아닌 메시지는, 예외를 던지지 않고 경고만 남긴다.")
    @ParameterizedTest
    @ValueSource(strings = "not-json")
    void givenInvalidJson_whenHandling_thenCatches(String message) {
        assertThatNoException().isThrownBy(() -> sut.handle(message));
    }
}
