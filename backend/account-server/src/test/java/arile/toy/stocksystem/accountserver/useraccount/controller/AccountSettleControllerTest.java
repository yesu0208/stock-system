package arile.toy.stocksystem.accountserver.useraccount.controller;

import arile.toy.stocksystem.accountserver.leverage.service.LeveragePositionSettleService;
import arile.toy.stocksystem.accountserver.useraccount.service.UserAccountService;
import arile.toy.stocksystem.accountserver.userstock.service.UserStockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountSettleController.class)
@ActiveProfiles("test")
class AccountSettleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccountService userAccountService;

    @MockitoBean
    private UserStockService userStockService;

    @MockitoBean
    private LeveragePositionSettleService leveragePositionSettleService;

    @Test
    @DisplayName("POST /internal/accounts/settle: 요청한 사용자들의 현금 → 주식 → 레버리지 순으로 정산한다")
    void settle() throws Exception {
        Set<String> usernames = Set.of("user1", "user2");

        mockMvc.perform(post("/internal/accounts/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernames": ["user1", "user2"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        InOrder inOrder = inOrder(userAccountService, userStockService, leveragePositionSettleService);
        inOrder.verify(userAccountService).settleAccounts(usernames);
        inOrder.verify(userStockService).settleStocks(usernames);
        inOrder.verify(leveragePositionSettleService).settleLeveragePositions(usernames);
    }

    @Test
    @DisplayName("POST /internal/accounts/settle: 요청 본문이 없으면 400을 반환하고 정산하지 않는다")
    void settle_withoutBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/internal/accounts/settle")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userAccountService, userStockService, leveragePositionSettleService);
    }

    @Test
    @DisplayName("POST /internal/accounts/settle-all: 전체 사용자를 현금 → 주식 → 레버리지 순으로 정산한다")
    void settleAll() throws Exception {
        mockMvc.perform(post("/internal/accounts/settle-all"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        InOrder inOrder = inOrder(userAccountService, userStockService, leveragePositionSettleService);
        inOrder.verify(userAccountService).settleAllAccounts();
        inOrder.verify(userStockService).settleAllStocks();
        inOrder.verify(leveragePositionSettleService).settleAllLeveragePositions();
    }
}
