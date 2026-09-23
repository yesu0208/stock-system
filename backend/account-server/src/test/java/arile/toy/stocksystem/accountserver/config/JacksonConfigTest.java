package arile.toy.stocksystem.accountserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final ObjectMapper mapper = new JacksonConfig().objectMapper();

    @Test
    @DisplayName("타임스탬프 직렬화가 비활성화되어 있다")
    void writeDatesAsTimestampsDisabled() {
        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    @DisplayName("Java Time 타입을 ISO-8601 문자열로 직렬화한다")
    void serializesJavaTimeAsIsoString() throws Exception {
        assertThat(mapper.writeValueAsString(LocalDate.of(2026, 1, 2)))
                .isEqualTo("\"2026-01-02\"");
        assertThat(mapper.writeValueAsString(LocalDateTime.of(2026, 1, 2, 9, 30, 0)))
                .isEqualTo("\"2026-01-02T09:30:00\"");
    }

    @Test
    @DisplayName("ISO-8601 문자열을 Java Time 타입으로 역직렬화한다")
    void deserializesIsoStringToJavaTime() throws Exception {
        assertThat(mapper.readValue("\"2026-01-02\"", LocalDate.class))
                .isEqualTo(LocalDate.of(2026, 1, 2));
    }
}
