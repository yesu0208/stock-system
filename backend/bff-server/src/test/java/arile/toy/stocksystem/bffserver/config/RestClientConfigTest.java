package arile.toy.stocksystem.bffserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTest {

    @Test
    @DisplayName("기본 설정의 RestClient를 생성한다")
    void restClient() {
        assertThat(new RestClientConfig().restClient()).isNotNull();
    }
}
