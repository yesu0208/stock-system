package arile.toy.stocksystem.bffserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    @Test
    @DisplayName("UTC 기준 시스템 시계를 생성한다 (KST 변환은 사용하는 쪽에서 처리)")
    void clock() {
        Instant before = Instant.now();

        Clock clock = new ClockConfig().clock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(clock.instant()).isBetween(before, Instant.now());
    }
}
