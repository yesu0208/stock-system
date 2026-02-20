package arile.toy.stocksystem.bffserver.cancel.controller;

import arile.toy.stocksystem.bffserver.cancel.dto.CancelRequest;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelResponse;
import arile.toy.stocksystem.bffserver.cancel.service.CancelIngressService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CancelController.class)
@AutoConfigureMockMvc(addFilters = false)
class CancelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CancelIngressService cancelIngressService;

    @MockitoBean
    private BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;

    @Test
    @DisplayName("장이 열려 있으면 취소에 성공한다")
    @WithMockUser(username = "user1", roles = "USER")
    void givenOpenMarket_whenCancel_thenReturnsOk() throws Exception {
        // given
        CancelRequest request =
                new CancelRequest(1L, "005930");

        CancelResponse response =
                new CancelResponse(1L, "005930");

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(false);

        given(cancelIngressService.receive(any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.stockCode").value("005930"));

        verify(cancelIngressService).receive(any());
    }

    @Test
    @DisplayName("장이 닫혀 있으면 예외가 발생한다")
    @WithMockUser(username = "user1", roles = "USER")
    void givenClosedMarket_whenCancel_thenThrowsException() throws Exception {
        // given
        CancelRequest request =
                new CancelRequest(1L, "005930");

        given(bffServerMarketPhaseRegistry.isClosed("005930"))
                .willReturn(true);

        // when & then
        mockMvc.perform(post("/api/v1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 401을 반환한다")
    void givenNoAuthentication_whenCancel_thenReturnsUnauthorized() throws Exception {
        // given
        CancelRequest request =
                new CancelRequest(1L, "005930");

        // when & then
        mockMvc.perform(post("/api/v1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
