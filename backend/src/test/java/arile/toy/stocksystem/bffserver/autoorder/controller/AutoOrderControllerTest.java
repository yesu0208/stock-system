package arile.toy.stocksystem.bffserver.autoorder.controller;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderRequest;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponse;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.service.AutoOrderIngressService;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutoOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutoOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AutoOrderIngressService autoOrderIngressService;

    @MockitoBean
    private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;

    @Test
    @DisplayName("장이 열려있고 인증된 사용자일 때 자동주문 요청하면 200 OK 반환")
    @WithMockUser(username = "user1", roles = "USER")
    void givenOpenMarketAndAuthenticatedUser_whenAutoOrder_thenReturnsOk() throws Exception {
        // given
        AutoOrderRequest request =
                new AutoOrderRequest("005930", AutoOrderType.BUY, 50000,
                        60000, 10);

        AutoOrderResponse response =
                new AutoOrderResponse("user1", "005930", AutoOrderType.BUY,
                        50000, 60000, 10);

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(false);

        given(autoOrderIngressService.receive(eq("user1"), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/auto-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.stockCode").value("005930"))
                .andExpect(jsonPath("$.autoOrderType").value(AutoOrderType.BUY.name()))
                .andExpect(jsonPath("$.triggerPrice").value(50000))
                .andExpect(jsonPath("$.orderPrice").value(60000))
                .andExpect(jsonPath("$.orderQuantity").value(10));

        verify(autoOrderIngressService)
                .receive(eq("user1"), any());
    }

    @Test
    @DisplayName("인증되지 않은 사용자일 때 자동주문 요청하면 401 반환")
    void givenUnauthenticatedUser_whenAutoOrder_thenReturnsUnauthorized() throws Exception {
        // given
        AutoOrderRequest request =
                new AutoOrderRequest("005930", AutoOrderType.BUY, 50000,
                        60000, 10);

        // when & then
        mockMvc.perform(post("/api/v1/auto-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(autoOrderIngressService, never())
                .receive(any(), any());
    }

    @Test
    @DisplayName("장이 닫혀있을 때 자동주문 요청하면 MarketClosedException 발생")
    @WithMockUser(username = "user1", roles = "USER")
    void givenClosedMarket_whenAutoOrder_thenThrowsMarketClosedException() throws Exception {
        // given
        AutoOrderRequest request =
                new AutoOrderRequest("005930", AutoOrderType.BUY, 50000,
                        60000, 10);

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(true);

        // when & then
        mockMvc.perform(post("/api/v1/auto-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(autoOrderIngressService, never())
                .receive(any(), any());
    }
}
