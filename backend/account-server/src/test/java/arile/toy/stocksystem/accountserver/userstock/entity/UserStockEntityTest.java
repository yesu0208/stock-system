package arile.toy.stocksystem.accountserver.userstock.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserStockEntityTest {

    @Test
    @DisplayName("of: 사용자·종목·매입금액·매입원금·수량으로 생성한다")
    void of() {
        UserStockEntity stock = UserStockEntity.of("user1", "005930", 700_000L, 700_105L, 10);

        assertThat(stock.getUsername()).isEqualTo("user1");
        assertThat(stock.getStockCode()).isEqualTo("005930");
        assertThat(stock.getAmount()).isEqualTo(700_000L);
        assertThat(stock.getCostAmount()).isEqualTo(700_105L);
        assertThat(stock.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("prePersist: 생성·수정 시각을 채운다")
    void prePersist() {
        UserStockEntity stock = UserStockEntity.of("user1", "005930", 700_000L, 700_105L, 10);

        ReflectionTestUtils.invokeMethod(stock, "prePersist");

        assertThat(stock.getCreatedDateTime()).isNotNull();
        assertThat(stock.getUpdatedDateTime()).isNotNull();
    }

    @Test
    @DisplayName("preUpdate: 수정 시각만 갱신한다")
    void preUpdate() {
        UserStockEntity stock = UserStockEntity.of("user1", "005930", 700_000L, 700_105L, 10);
        Instant created = Instant.parse("2026-09-01T00:00:00Z");
        stock.setCreatedDateTime(created);
        stock.setUpdatedDateTime(created);

        ReflectionTestUtils.invokeMethod(stock, "preUpdate");

        assertThat(stock.getCreatedDateTime()).isEqualTo(created);
        assertThat(stock.getUpdatedDateTime()).isAfter(created);
    }
}
