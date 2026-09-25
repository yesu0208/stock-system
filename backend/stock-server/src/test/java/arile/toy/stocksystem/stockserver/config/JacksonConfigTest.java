package arile.toy.stocksystem.stockserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Config] Jackson 설정 테스트")
class JacksonConfigTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    record Sample(String name, Instant at, LocalDate date) {
    }

    @Test
    @DisplayName("날짜·시각을 숫자가 아닌 ISO 문자열로 쓴다")
    void writesIsoDates() throws Exception {
        String json = objectMapper.writeValueAsString(
                new Sample("x", Instant.parse("2026-09-25T00:30:00Z"), LocalDate.of(2026, 9, 25)));

        assertThat(json).contains("\"at\":\"2026-09-25T00:30:00Z\"", "\"date\":\"2026-09-25\"");
    }

    @Test
    @DisplayName("ISO 문자열 날짜·시각을 읽는다")
    void readsIsoDates() throws Exception {
        Sample sample = objectMapper.readValue(
                "{\"name\": \"x\", \"at\": \"2026-09-25T00:30:00Z\", \"date\": \"2026-09-25\"}", Sample.class);

        assertThat(sample).isEqualTo(
                new Sample("x", Instant.parse("2026-09-25T00:30:00Z"), LocalDate.of(2026, 9, 25)));
    }
}
