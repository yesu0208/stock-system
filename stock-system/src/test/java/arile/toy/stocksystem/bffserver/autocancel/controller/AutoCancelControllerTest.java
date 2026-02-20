package arile.toy.stocksystem.bffserver.autocancel.controller;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelRequest;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResponse;
import arile.toy.stocksystem.bffserver.autocancel.service.AutoCancelIngressService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AutoCancelController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutoCancelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AutoCancelIngressService autoCancelIngressService;

    @MockitoBean
    private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;

    @Test
    @DisplayName("장이 열려 있으면 자동취소에 성공한다")
    @WithMockUser(username = "user1", roles = "USER")
    void givenOpenMarket_whenAutoCancel_thenReturnsOk() throws Exception {
        // given
        AutoCancelRequest request = new AutoCancelRequest(1L, "005930");

        AutoCancelResponse response = new AutoCancelResponse(1L, "005930");

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(false);

        given(autoCancelIngressService.receive(any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/auto-orders/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoOrderId").value(1L))
                .andExpect(jsonPath("$.stockCode").value("005930"));

        verify(autoCancelIngressService).receive(any());
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void givenNoAuthentication_whenAutoCancel_thenReturnsUnauthorized() throws Exception {
        // given
        AutoCancelRequest request = new AutoCancelRequest(1L, "005930");

        // when & then
        mockMvc.perform(post("/api/v1/auto-orders/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(autoCancelIngressService, never()).receive(any());
    }

    @Test
    @DisplayName("장이 닫혀 있으면 MarketClosedException이 발생한다")
    @WithMockUser(username = "user1", roles = "USER")
    void givenClosedMarket_whenAutoCancel_thenThrowsMarketClosedException() throws Exception {
        // given
        AutoCancelRequest request = new AutoCancelRequest(1L, "005930");

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(true);

        // when & then
        mockMvc.perform(post("/api/v1/auto-orders/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(autoCancelIngressService, never()).receive(any());
    }
}
