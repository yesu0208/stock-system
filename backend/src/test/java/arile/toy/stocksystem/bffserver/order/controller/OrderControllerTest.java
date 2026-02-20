package arile.toy.stocksystem.bffserver.order.controller;

import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.order.dto.OrderRequest;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.service.OrderIngressService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private OrderIngressService orderIngressService;

    @MockitoBean
    private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;

    @Test
    @DisplayName("장이 열려 있으면 주문에 성공")
    @WithMockUser(username = "user1", roles = "USER")
    void givenOpenMarket_whenOrder_thenReturnsOk() throws Exception {
        // given
        OrderRequest request =
                new OrderRequest("005930", OrderType.BUY, 50000, 50);

        OrderResponse response =
                new OrderResponse("user1", "005930", OrderType.BUY, 50000, 50);

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(false);

        given(orderIngressService.receive(any(), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.stockCode").value("005930"))
                .andExpect(jsonPath("$.orderType").value(OrderType.BUY.name()))
                .andExpect(jsonPath("$.orderPrice").value(50000))
                .andExpect(jsonPath("$.orderQuantity").value(50));

        verify(orderIngressService).receive(any(), any());
    }

    @Test
    @DisplayName("장이 닫혀 있으면 예외가 발생")
    @WithMockUser(username = "user1", roles = "USER")
    void givenClosedMarket_whenOrder_thenThrowsException() throws Exception {
        // given
        OrderRequest request =
                new OrderRequest("005930", OrderType.BUY, 1000000, 50);

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(true);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 401을 반환")
    void givenNoAuthentication_whenOrder_thenReturnsUnauthorized() throws Exception {
        // given
        OrderRequest request =
                new OrderRequest("005930", OrderType.BUY, 1000000, 50);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
