package arile.toy.stocksystem.stockserver.alert.dto;

import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alert.event.AlertFiredEvent;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.event.AlertResponseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 알림 변환 테스트")
class AlertDtoTest {

    @DisplayName("엔티티를 DTO로 변환하고, 신규 엔티티는 등록 시각을 가진다")
    @Test
    void whenConvertingEntity_thenMapsFields() {
        AlertEntity entity = AlertEntity.of("user", "005930", AlertDirection.ABOVE, 72_000, AlertStatus.ACTIVE);
        entity.setAlertId(1L);

        AlertDto dto = AlertDto.fromEntity(entity);

        assertThat(dto).isEqualTo(new AlertDto(1L, "user", "005930", AlertDirection.ABOVE, 72_000, entity.getRegisteredTime()));
        assertThat(entity.getRegisteredTime()).isNotNull();
    }

    @DisplayName("응답 이벤트: 등록 성공은 메시지 값으로, 실패는 요청 값과 에러 코드로 만든다")
    @Test
    void whenCreatingResponseEvents_thenMapsFields() {
        Instant time = Instant.parse("2026-09-25T00:00:00Z");
        var message = StockServerAlertResponseMessage.of(1L, "user", "005930", AlertDirection.BELOW, 68_000, time);

        AlertResponseEvent success = AlertResponseEvent.fromResponseMessage(message);
        AlertResponseEvent error = AlertResponseEvent.error(
                AlertRequestEvent.of("user", "005930", AlertDirection.BELOW, 68_000), AlertErrorCode.INTERNAL_ERROR);

        assertThat(success).isEqualTo(new AlertResponseEvent(1L, "user", "005930", AlertDirection.BELOW, 68_000, time, true, null));
        assertThat(error).isEqualTo(new AlertResponseEvent(null, "user", "005930", AlertDirection.BELOW, 68_000, null,
                false, AlertErrorCode.INTERNAL_ERROR));
    }

    @DisplayName("발송 이벤트: 알림 값과 현재가, 발송 시각을 담는다")
    @Test
    void whenCreatingFiredEvent_thenMapsFields() {
        AlertFiredEvent event = AlertFiredEvent.of(
                new AlertDto(1L, "user", "005930", AlertDirection.ABOVE, 72_000, Instant.now()), 72_100);

        assertThat(event.alertId()).isEqualTo(1L);
        assertThat(event.triggerPrice()).isEqualTo(72_000);
        assertThat(event.currentPrice()).isEqualTo(72_100);
        assertThat(event.firedTime()).isNotNull();
    }
}
