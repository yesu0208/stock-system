package arile.toy.stocksystem.stockserver.external.stock.approvalkey;

import arile.toy.stocksystem.stockserver.exception.ApprovalKeyIssuanceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class ApprovalKeyService {

    @Value("${api.app-key}")
    private String appKey;

    @Value("${api.app-secret}")
    private String appSecret;

    @Value("${api.approval-key-url}")
    private String approvalKeyUrl;

    private final RestClient restClient = RestClient.create();

    public String issueApprovalKey() {

        ApprovalKeyResponse approvalKeyResponse = restClient
                .post()
                .uri(approvalKeyUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", appKey,
                        "secretkey", appSecret
                ))
                .retrieve()
                .body(ApprovalKeyResponse.class);


        if (approvalKeyResponse == null || approvalKeyResponse.approvalKey() == null) {
            throw new ApprovalKeyIssuanceException("approval_key issue failed.");
        }

        log.info("approval_key issue success.");
        return approvalKeyResponse.approvalKey();
    }
}