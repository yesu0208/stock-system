package arile.toy.stocksystem;

import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.stockserver.external.stock.approvalkey.ApprovalKeyService;
import arile.toy.stocksystem.stockserver.external.stock.checker.ExternalStockWebSocketOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class StockSystemApplicationTests {

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ApprovalKeyService approvalKeyService;

    @MockitoBean
    private ExternalStockWebSocketOrchestrator externalStockWebSocketOrchestrator;
    
    @MockitoBean
    private RedisMessageListenerContainer redisContainer;

    @Test
    void contextLoads() {

    }

}
