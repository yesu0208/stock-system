package arile.toy.stocksystem.bffserver.watchlist.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WatchListEntityTest {

    @Test
    @DisplayName("of로 사용자·종목·순서를 채우고, 저장 전 생성 시각을 채운다")
    void ofAndPrePersist() {
        WatchListEntity entity = WatchListEntity.of("user1", "005930", "삼성전자", 2);

        ReflectionTestUtils.invokeMethod(entity, "prePersist");

        assertThat(entity.getUsername()).isEqualTo("user1");
        assertThat(entity.getStockCode()).isEqualTo("005930");
        assertThat(entity.getStockName()).isEqualTo("삼성전자");
        assertThat(entity.getSortOrder()).isEqualTo(2);
        assertThat(entity.getCreatedDateTime()).isNotNull();
    }
}
