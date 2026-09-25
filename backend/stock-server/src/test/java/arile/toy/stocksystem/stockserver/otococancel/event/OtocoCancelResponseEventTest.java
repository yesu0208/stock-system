package arile.toy.stocksystem.stockserver.otococancel.event;

import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otococancel.dto.OtocoCancelErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Event] OTOCO 취소 응답 이벤트 변환 테스트")
class OtocoCancelResponseEventTest {

    @DisplayName("엔티티 값과 성공 여부·에러 코드로 이벤트를 만든다")
    @Test
    void whenCreating_thenMapsFields() {
        OtocoCancelResponseEvent event = OtocoCancelResponseEvent.of(
                OtocoFixtures.entity(OtocoStatus.CANCELED), false, OtocoCancelErrorCode.ALREADY_CANCELLED);

        assertThat(event.otocoId()).isEqualTo(1L);
        assertThat(event.username()).isEqualTo("user");
        assertThat(event.stockCode()).isEqualTo("005930");
        assertThat(event.entryDirection()).isEqualTo(OtocoEntryDirection.BELOW);
        assertThat(event.entryTriggerPrice()).isEqualTo(70_000);
        assertThat(event.orderQuantity()).isEqualTo(10);
        assertThat(event.success()).isFalse();
        assertThat(event.errorCode()).isEqualTo(OtocoCancelErrorCode.ALREADY_CANCELLED);
    }
}
