package arile.toy.stocksystem.bffserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    record Sample(String name, Instant at, LocalDate date) {
    }

    @Test
    @DisplayName("날짜·시각을 숫자가 아닌 ISO 문자열로 쓴다 (프론트와 다른 서버가 같은 형식으로 읽음)")
    void writesIsoDates() throws Exception {
        String json = objectMapper.writeValueAsString(
                new Sample("x", Instant.parse("2026-09-24T00:30:00Z"), LocalDate.of(2026, 9, 24)));

        assertThat(json).contains("\"at\":\"2026-09-24T00:30:00Z\"", "\"date\":\"2026-09-24\"");
    }

    @Test
    @DisplayName("ISO 문자열 날짜·시각을 읽는다")
    void readsIsoDates() throws Exception {
        Sample sample = objectMapper.readValue(
                "{\"name\": \"x\", \"at\": \"2026-09-24T00:30:00Z\", \"date\": \"2026-09-24\"}", Sample.class);

        assertThat(sample).isEqualTo(
                new Sample("x", Instant.parse("2026-09-24T00:30:00Z"), LocalDate.of(2026, 9, 24)));
    }

    @Test
    @DisplayName("모르는 필드가 있어도 무시하고 읽는다 (다른 서버가 이벤트에 필드를 추가해 먼저 배포되는 경우)")
    void ignoresUnknownFields() throws Exception {
        Sample sample = objectMapper.readValue(
                "{\"name\": \"x\", \"at\": \"2026-09-24T00:30:00Z\", \"date\": \"2026-09-24\", \"fee\": 150}",
                Sample.class);

        assertThat(sample.name()).isEqualTo("x");
    }
}
