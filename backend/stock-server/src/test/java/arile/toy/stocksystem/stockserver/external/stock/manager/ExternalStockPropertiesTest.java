package arile.toy.stocksystem.stockserver.external.stock.manager;

import arile.toy.stocksystem.stockserver.sharding.StockGroupProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Properties] 담당 종목 조회 테스트")
class ExternalStockPropertiesTest {

    @DisplayName("내 그룹 종목과 CLOSE 그룹 종목을 돌려주고, 없으면 빈 목록")
    @Test
    void whenGettingStocks_thenByGroup() {
        StockGroupProperties groups = new StockGroupProperties();
        groups.setGroups(Map.of("A", List.of("005930"), "CLOSE", List.of("999999")));
        ExternalStockProperties sut = new ExternalStockProperties(groups);
        ReflectionTestUtils.setField(sut, "myGroup", "A");

        assertThat(sut.getOpen()).containsExactly("005930");
        assertThat(sut.getClose()).containsExactly("999999");

        ReflectionTestUtils.setField(sut, "myGroup", "B");
        groups.setGroups(Map.of());
        assertThat(sut.getOpen()).isEmpty();
        assertThat(sut.getClose()).isEmpty();
    }
}
