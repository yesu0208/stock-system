package arile.toy.stocksystem.stockserver.sharding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("[Sharding] 종목 → 서버 그룹 매핑 테스트")
class StockGroupRegistryTest {

    @DisplayName("설정된 종목은 그룹을 돌려주고, 없는 종목은 예외를 던진다")
    @Test
    void whenResolving_thenMapsOrThrows() {
        StockGroupProperties properties = new StockGroupProperties();
        properties.setGroups(Map.of("A", List.of("005930", "000660"), "B", List.of("035420")));
        StockGroupRegistry sut = new StockGroupRegistry(properties);
        sut.init();

        assertThat(sut.resolveGroup("005930")).isEqualTo("A");
        assertThat(sut.resolveGroup("035420")).isEqualTo("B");
        assertThatThrownBy(() -> sut.resolveGroup("999999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("999999");
    }
}
