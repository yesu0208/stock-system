package arile.toy.stocksystem.stockserver.external.stock.approvalkey;

import arile.toy.stocksystem.stockserver.exception.ApprovalKeyIssuanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("[Service] 외부 시세 승인키 발급 테스트")
class ApprovalKeyServiceTest {

    private static final String URL = "http://kis/oauth2/Approval";

    private MockRestServiceServer server;
    private ApprovalKeyService sut;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sut = new ApprovalKeyService(builder.build());
        ReflectionTestUtils.setField(sut, "appKey", "app-key");
        ReflectionTestUtils.setField(sut, "appSecret", "app-secret");
        ReflectionTestUtils.setField(sut, "approvalKeyUrl", URL);
    }

    @DisplayName("앱 키·시크릿으로 요청해 approval_key를 돌려준다")
    @Test
    void whenIssuing_thenReturnsKey() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"grant_type\":\"client_credentials\",\"appkey\":\"app-key\",\"secretkey\":\"app-secret\"}"))
                .andRespond(withSuccess("{\"approval_key\":\"abc\"}", MediaType.APPLICATION_JSON));

        assertThat(sut.issueApprovalKey()).isEqualTo("abc");
        server.verify();
    }

    @DisplayName("응답에 approval_key가 없으면 예외를 던진다")
    @Test
    void givenNoKey_whenIssuing_thenThrows() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> sut.issueApprovalKey()).isInstanceOf(ApprovalKeyIssuanceException.class);
    }
}
