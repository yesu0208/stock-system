package arile.toy.stocksystem.stockserver.external.stock.approvalkey;

import arile.toy.stocksystem.stockserver.exception.ApprovalKeyIssuanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ApprovalKeyServiceTest {

    private static final String URL = "https://api.example.com/oauth2/Approval";

    private MockRestServiceServer server;
    private ApprovalKeyService sut;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sut = new ApprovalKeyService(builder.build());
        ReflectionTestUtils.setField(sut, "appKey", "key");
        ReflectionTestUtils.setField(sut, "appSecret", "secret");
        ReflectionTestUtils.setField(sut, "approvalKeyUrl", URL);
    }

    @DisplayName("승인키가 정상 응답되면, 승인키를 반환한다.")
    @Test
    void givenValidResponse_whenIssuing_thenReturnsKey() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.grant_type").value("client_credentials"))
                .andExpect(jsonPath("$.appkey").value("key"))
                .andExpect(jsonPath("$.secretkey").value("secret"))
                .andRespond(withSuccess("{\"approval_key\":\"abc\"}", MediaType.APPLICATION_JSON));

        assertThat(sut.issueApprovalKey()).isEqualTo("abc");
        server.verify();
    }

    @DisplayName("응답이 없거나 승인키가 없으면, 예외를 던진다.")
    @ParameterizedTest
    @ValueSource(strings = {"null", "{}"})
    void givenNoResponseOrNoKey_whenIssuing_thenThrows(String body) {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> sut.issueApprovalKey())
                .isInstanceOf(ApprovalKeyIssuanceException.class)
                .hasMessage("approval_key issue failed.");
    }
}
