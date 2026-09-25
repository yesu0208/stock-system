package arile.toy.stocksystem.stockserver.alertcancel.event;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.AlertStatus;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alertcancel.dto.AlertCancelErrorCode;
import arile.toy.stocksystem.stockserver.alertcancel.entity.AlertCancelEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Event] 알림 취소 응답 이벤트·이력 엔티티 생성 테스트")
class AlertCancelResponseEventTest {

    @DisplayName("엔티티 값과 성공 여부·에러 코드로 이벤트를 만든다")
    @Test
    void whenCreatingEvent_thenMapsFields() {
        AlertEntity entity = AlertEntity.of("user", "005930", AlertDirection.BELOW, 68_000, AlertStatus.CANCELED);
        entity.setAlertId(1L);

        AlertCancelResponseEvent event = AlertCancelResponseEvent.of(entity, false, AlertCancelErrorCode.ALREADY_FIRED);

        assertThat(event).isEqualTo(new AlertCancelResponseEvent(1L, "user", "005930", AlertDirection.BELOW, 68_000,
                false, AlertCancelErrorCode.ALREADY_FIRED));
    }

    @DisplayName("취소 이력은 알림 id와 취소 시각을 가진다")
    @Test
    void whenCreatingCancelEntity_thenHasIdAndTime() {
        AlertCancelEntity entity = AlertCancelEntity.of(1L);

        assertThat(entity.getAlertId()).isEqualTo(1L);
        assertThat(entity.getCancelTime()).isNotNull();
    }
}
