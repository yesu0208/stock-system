package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.LeveragePositionInfo;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AccountPullServiceTest {

    private static final String ACCOUNT_KEY = "account:user1";
    private static final String LEVERAGE_KEY = "account:leverage:user1";

    private HashOperations<String, Object, Object> hashOps;
    private AccountPullService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        hashOps = mock(HashOperations.class);
        doReturn(hashOps).when(redisTemplate).opsForHash();
        service = new AccountPullService(redisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("Redis에 계좌가 없으면 RedisAccountNotFoundException을 던진다")
    void accountNotFound() {
        given(hashOps.entries(ACCOUNT_KEY)).willReturn(Map.of());

        assertThatThrownBy(() -> service.getAccountMessage("user1"))
                .isInstanceOf(RedisAccountNotFoundException.class)
                .hasMessageContaining("user1");
    }

    @Test
    @DisplayName("현금·현물·계좌 상태와 레버리지 포지션·마진 상태를 읽어 스냅샷으로 만든다")
    void fullSnapshot() {
        given(hashOps.entries(ACCOUNT_KEY)).willReturn(Map.of(
                "availableCash", "1000000",
                "reservedCash", "200000",
                "stocks", """
                        {"005930": {"quantity": 10, "availableQuantity": 8, "totalAmount": 700000, "totalCostAmount": 700105}}
                        """,
                "accountStatus", "NEGATIVE"));
        given(hashOps.get(LEVERAGE_KEY, "positions")).willReturn("""
                {"000660:X2": {"quantity": 5, "availableQuantity": 5, "purchaseAmount": 600000,
                               "costAmount": 600090, "loanAmount": 300000, "marginStatus": "MARGIN_CALL"}}
                """);
        given(hashOps.get(LEVERAGE_KEY, "marginStatus")).willReturn("MARGIN_CALL");

        AccountSnapshot snapshot = service.getAccountMessage("user1");

        assertThat(snapshot.availableCash()).isEqualTo(1_000_000L);
        assertThat(snapshot.reservedCash()).isEqualTo(200_000L);
        assertThat(snapshot.stocks()).containsEntry("005930", new StockInfo(10, 8, 700_000L, 700_105L));
        assertThat(snapshot.leveragePositions()).containsEntry("000660:X2",
                new LeveragePositionInfo(5, 5, 600_000L, 600_090L, 300_000L, "MARGIN_CALL"));
        assertThat(snapshot.marginStatus()).isEqualTo("MARGIN_CALL");
        assertThat(snapshot.accountStatus()).isEqualTo("NEGATIVE");
    }

    @Test
    @DisplayName("값이 없으면 현금 0, 빈 보유 목록, 상태 NORMAL로 채운다 (null 맵을 반환하지 않음)")
    void defaults() {
        given(hashOps.entries(ACCOUNT_KEY)).willReturn(Map.of("availableCash", "500000"));
        given(hashOps.get(LEVERAGE_KEY, "positions")).willReturn(null);
        given(hashOps.get(LEVERAGE_KEY, "marginStatus")).willReturn(null);

        AccountSnapshot snapshot = service.getAccountMessage("user1");

        assertThat(snapshot.availableCash()).isEqualTo(500_000L);
        assertThat(snapshot.reservedCash()).isZero();
        assertThat(snapshot.stocks()).isNotNull().isEmpty();
        assertThat(snapshot.leveragePositions()).isNotNull().isEmpty();
        assertThat(snapshot.marginStatus()).isEqualTo("NORMAL");
        assertThat(snapshot.accountStatus()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("보유 목록 JSON이 비었거나 깨져 있으면 빈 목록으로 처리한다")
    void blankOrBrokenJson() {
        given(hashOps.entries(ACCOUNT_KEY)).willReturn(Map.of(
                "availableCash", "0", "reservedCash", "0", "stocks", "  "));
        given(hashOps.get(LEVERAGE_KEY, "positions")).willReturn("{broken json");
        given(hashOps.get(LEVERAGE_KEY, "marginStatus")).willReturn(null);

        AccountSnapshot snapshot = service.getAccountMessage("user1");

        assertThat(snapshot.stocks()).isEmpty();
        assertThat(snapshot.leveragePositions()).isEmpty();
    }
}
