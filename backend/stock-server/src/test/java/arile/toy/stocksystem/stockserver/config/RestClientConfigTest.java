package arile.toy.stocksystem.stockserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Config] RestClient 설정 테스트")
class RestClientConfigTest {

    @Test
    @DisplayName("타임아웃이 설정된 RestClient를 생성한다")
    void restClient() {
        assertThat(new RestClientConfig().restClient()).isNotNull();
    }
}
