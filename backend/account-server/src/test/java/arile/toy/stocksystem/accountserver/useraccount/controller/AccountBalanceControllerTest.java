package arile.toy.stocksystem.accountserver.useraccount.controller;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.service.LeveragePositionApplyService;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.service.AccountStatusGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountBalanceController.class)
@ActiveProfiles("test")
class AccountBalanceControllerTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountBalanceCommand accountBalanceCommand;

    @MockitoBean
    private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @MockitoBean
    private LeveragePositionApplyService leveragePositionApplyService;

    @MockitoBean
    private AccountStatusGuard accountStatusGuard;

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post("/internal/accounts/" + USERNAME + path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void expectSuccess(ResultActions result, boolean success) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(success));
    }

    private void verifyPublished(boolean success) {
        verify(accountUpdateEventPublisher, times(success ? 1 : 0)).publish(USERNAME);
    }

    // ===== 현금 =====

    @Nested
    @DisplayName("POST /{username}/reserve-cash")
    class ReserveCash {

        private static final String BODY = """
                {"amount": 70000}
                """;

        @Test
        @DisplayName("매수가 허용되지 않는 계좌 상태면 예약하지 않고 false를 반환한다")
        void whenBuyNotAllowed_returnsFalse() throws Exception {
            given(accountStatusGuard.allowBuy(USERNAME)).willReturn(false);

            expectSuccess(postJson("/reserve-cash", BODY), false);

            verifyNoInteractions(accountBalanceCommand, accountUpdateEventPublisher);
        }

        @ParameterizedTest(name = "예약 결과={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("현금 예약 결과를 반환하고 성공 시에만 계좌 이벤트를 발행한다")
        void reserveCash(boolean success) throws Exception {
            given(accountStatusGuard.allowBuy(USERNAME)).willReturn(true);
            given(accountBalanceCommand.reserveCash(USERNAME, 70000L)).willReturn(success);

            expectSuccess(postJson("/reserve-cash", BODY), success);

            verifyPublished(success);
        }
    }

    @ParameterizedTest(name = "환불 결과={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("POST /{username}/refund-cash: 예약 현금을 환불하고 성공 시에만 이벤트를 발행한다")
    void refundCash(boolean success) throws Exception {
        given(accountBalanceCommand.refundReservedCash(USERNAME, 30000L)).willReturn(success);

        expectSuccess(postJson("/refund-cash", """
                {"amount": 30000}
                """), success);

        verifyPublished(success);
        verifyNoInteractions(accountStatusGuard);
    }

    // ===== 현물 주식 =====

    @Nested
    @DisplayName("POST /{username}/reserve-stock")
    class ReserveStock {

        private static final String BODY = """
                {"stockCode": "005930", "quantity": 10}
                """;

        @Test
        @DisplayName("매도가 허용되지 않는 계좌 상태면 예약하지 않고 false를 반환한다")
        void whenSellNotAllowed_returnsFalse() throws Exception {
            given(accountStatusGuard.allowSell(USERNAME)).willReturn(false);

            expectSuccess(postJson("/reserve-stock", BODY), false);

            verifyNoInteractions(accountBalanceCommand, accountUpdateEventPublisher);
        }

        @ParameterizedTest(name = "예약 결과={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("주식 예약 결과를 반환하고 성공 시에만 계좌 이벤트를 발행한다")
        void reserveStock(boolean success) throws Exception {
            given(accountStatusGuard.allowSell(USERNAME)).willReturn(true);
            given(accountBalanceCommand.reserveStock(USERNAME, STOCK_CODE, 10)).willReturn(success);

            expectSuccess(postJson("/reserve-stock", BODY), success);

            verifyPublished(success);
        }
    }

    @ParameterizedTest(name = "환불 결과={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("POST /{username}/refund-stock: 예약 주식을 환불하고 성공 시에만 이벤트를 발행한다")
    void refundStock(boolean success) throws Exception {
        given(accountBalanceCommand.refundReservedStock(USERNAME, STOCK_CODE, 4)).willReturn(success);

        expectSuccess(postJson("/refund-stock", """
                {"stockCode": "005930", "quantity": 4}
                """), success);

        verifyPublished(success);
        verifyNoInteractions(accountStatusGuard);
    }

    // ===== 레버리지 주식 =====

    @Nested
    @DisplayName("POST /{username}/reserve-leverage-stock")
    class ReserveLeverageStock {

        private static final String BODY = """
                {"stockCode": "005930", "leverageRatio": "X2", "quantity": 5}
                """;

        @Test
        @DisplayName("매도가 허용되지 않는 계좌 상태면 예약하지 않고 false를 반환한다")
        void whenSellNotAllowed_returnsFalse() throws Exception {
            given(accountStatusGuard.allowSell(USERNAME)).willReturn(false);

            expectSuccess(postJson("/reserve-leverage-stock", BODY), false);

            verifyNoInteractions(leveragePositionApplyService, accountUpdateEventPublisher);
        }

        @ParameterizedTest(name = "예약 결과={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("문자열 배율을 LeverageRatio로 변환해 예약하고 성공 시에만 이벤트를 발행한다")
        void reserveLeverageStock(boolean success) throws Exception {
            given(accountStatusGuard.allowSell(USERNAME)).willReturn(true);
            given(leveragePositionApplyService.reserveLeverageStock(
                    USERNAME, STOCK_CODE, LeverageRatio.X2, 5)).willReturn(success);

            expectSuccess(postJson("/reserve-leverage-stock", BODY), success);

            verifyPublished(success);
        }
    }

    @ParameterizedTest(name = "환불 결과={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("POST /{username}/refund-leverage-stock: 레버리지 예약 수량을 환불하고 성공 시에만 이벤트를 발행한다")
    void refundLeverageStock(boolean success) throws Exception {
        given(leveragePositionApplyService.refundReservedLeverageStock(
                USERNAME, STOCK_CODE, LeverageRatio.X1_5, 2)).willReturn(success);

        expectSuccess(postJson("/refund-leverage-stock", """
                {"stockCode": "005930", "leverageRatio": "X1_5", "quantity": 2}
                """), success);

        verifyPublished(success);
        verifyNoInteractions(accountStatusGuard);
    }
}
