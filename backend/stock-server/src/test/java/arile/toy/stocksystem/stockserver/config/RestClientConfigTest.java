package arile.toy.stocksystem.stockserver.config;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Config] RestClient 설정 테스트")
class RestClientConfigTest {

    @Test
    @DisplayName("타임아웃이 설정된 RestClient를 생성한다")
    void restClient() {
        var observationRegistry = new DefaultListableBeanFactory().getBeanProvider(ObservationRegistry.class);

        assertThat(new RestClientConfig().restClient(observationRegistry)).isNotNull();
    }
}
