package arile.toy.stocksystem.stockserver.order.controller;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.OrderHistoryItem;
import arile.toy.stocksystem.stockserver.order.service.OrderHistoryQueryService;
import arile.toy.stocksystem.stockserver.trade.dto.TradeHistoryItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[Controller] 주문·체결 내역 내부 API 테스트")
@WebMvcTest(OrderHistoryController.class)
class OrderHistoryControllerTest {

    private static final String USERNAME = "user";
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-25T00:00:00Z");

    @Autowired private MockMvc mvc;

    @MockitoBean private OrderHistoryQueryService orderHistoryQueryService;

    @DisplayName("주문 내역: 조건 파라미터(종목, ISO 기간, 페이지)를 서비스에 그대로 넘기고 페이지 응답을 반환한다")
    @Test
    void givenAllParams_whenGettingHistory_thenPassesParamsAndReturnsPage() throws Exception {
        // Given
        given(orderHistoryQueryService.getOrderHistory(USERNAME, "005930", FROM, TO, PageRequest.of(2, 10)))
                .willReturn(new HistoryPageResponse<>(List.of(), 2, 10, 25L, false));

        // When & Then
        mvc.perform(get("/internal/orders/{username}/history", USERNAME)
                        .param("stockCode", "005930")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-25T00:00:00Z")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @DisplayName("주문 내역: 선택 파라미터가 없으면 null 조건과 기본 페이지(0, 20)로 조회한다")
    @Test
    void givenNoParams_whenGettingHistory_thenUsesDefaults() throws Exception {
        // Given
        given(orderHistoryQueryService.getOrderHistory(USERNAME, null, null, null, PageRequest.of(0, 20)))
                .willReturn(emptyOrderPage());

        // When & Then
        mvc.perform(get("/internal/orders/{username}/history", USERNAME))
                .andExpect(status().isOk());

        then(orderHistoryQueryService).should()
                .getOrderHistory(USERNAME, null, null, null, PageRequest.of(0, 20));
    }

    @DisplayName("취소 내역: 취소 내역 조회로 위임한다")
    @Test
    void givenRequest_whenGettingCancels_thenDelegatesToCancelHistory() throws Exception {
        // Given
        given(orderHistoryQueryService.getCancelHistory(USERNAME, null, null, null, PageRequest.of(0, 20)))
                .willReturn(emptyOrderPage());

        // When & Then
        mvc.perform(get("/internal/orders/{username}/cancels", USERNAME))
                .andExpect(status().isOk());

        then(orderHistoryQueryService).should()
                .getCancelHistory(USERNAME, null, null, null, PageRequest.of(0, 20));
    }

    @DisplayName("미체결 주문: 미체결 조회로 위임한다")
    @Test
    void givenRequest_whenGettingUnfilled_thenDelegatesToUnfilledOrders() throws Exception {
        // Given
        given(orderHistoryQueryService.getUnfilledOrders(USERNAME, null, null, null, PageRequest.of(0, 20)))
                .willReturn(emptyOrderPage());

        // When & Then
        mvc.perform(get("/internal/orders/{username}/unfilled", USERNAME))
                .andExpect(status().isOk());

        then(orderHistoryQueryService).should()
                .getUnfilledOrders(USERNAME, null, null, null, PageRequest.of(0, 20));
    }

    @DisplayName("체결 내역: 체결 내역 조회로 위임한다")
    @Test
    void givenRequest_whenGettingTrades_thenDelegatesToTradeHistory() throws Exception {
        // Given
        given(orderHistoryQueryService.getTradeHistory(USERNAME, null, null, null, PageRequest.of(0, 20)))
                .willReturn(new HistoryPageResponse<TradeHistoryItem>(List.of(), 0, 20, 0L, false));

        // When & Then
        mvc.perform(get("/internal/orders/{username}/trades", USERNAME))
                .andExpect(status().isOk());

        then(orderHistoryQueryService).should()
                .getTradeHistory(USERNAME, null, null, null, PageRequest.of(0, 20));
    }

    @DisplayName("기간 파라미터가 ISO 날짜시간 형식이 아니면 400을 반환하고 서비스를 호출하지 않는다")
    @Test
    void givenInvalidDate_whenGettingHistory_thenBadRequest() throws Exception {
        mvc.perform(get("/internal/orders/{username}/history", USERNAME)
                        .param("from", "2026/09/01"))
                .andExpect(status().isBadRequest());

        then(orderHistoryQueryService).shouldHaveNoInteractions();
    }

    private HistoryPageResponse<OrderHistoryItem> emptyOrderPage() {
        return new HistoryPageResponse<>(List.of(), 0, 20, 0L, false);
    }
}
