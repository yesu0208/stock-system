package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    @DisplayName("접수·일부 체결은 열린 주문이다 (대기열 복구·미체결 조회 대상)")
    void openStatuses() {
        assertThat(OrderStatus.OPEN.isOpen()).isTrue();
        assertThat(OrderStatus.PARTIAL.isOpen()).isTrue();
    }

    @Test
    @DisplayName("전량 체결·취소는 끝난 주문이다 (다시 체결되거나 취소되지 않음)")
    void closedStatuses() {
        assertThat(OrderStatus.FILLED.isOpen()).isFalse();
        assertThat(OrderStatus.CANCELED.isOpen()).isFalse();
    }

    @Test
    @DisplayName("열린 상태는 정확히 접수·일부 체결 두 가지다 (상태 추가 시 열림 여부를 반드시 정하도록)")
    void openSetIsExact() {
        assertThat(Arrays.stream(OrderStatus.values()).filter(OrderStatus::isOpen))
                .containsExactlyInAnyOrder(OrderStatus.OPEN, OrderStatus.PARTIAL);
    }
}
