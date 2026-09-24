package arile.toy.stocksystem.bffserver.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderErrorCodeTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(OrderErrorCode.class)
    @DisplayName("일반 주문 오류 문구는 [주문 거절]로 시작하고, 자동 주문 문구가 섞여 있지 않다")
    void messagesAreForRegularOrders(OrderErrorCode errorCode) {
        assertThat(errorCode.userMessage())
                .startsWith("[주문 거절]")
                .doesNotContain("자동 주문");
    }

    @Test
    @DisplayName("보유 주식 부족: 일반 매도 주문 거절 안내")
    void insufficientStock() {
        assertThat(OrderErrorCode.INSUFFICIENT_STOCK.userMessage())
                .isEqualTo("[주문 거절] 보유 주식이 부족하여 주문에 실패했습니다.");
    }
}
