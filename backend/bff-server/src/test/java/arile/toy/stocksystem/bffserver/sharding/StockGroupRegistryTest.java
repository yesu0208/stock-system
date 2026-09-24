package arile.toy.stocksystem.bffserver.sharding;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockGroupRegistryTest {

    private static StockGroupRegistry registry(Map<String, List<String>> groups) {
        StockGroupProperties properties = new StockGroupProperties();
        properties.setGroups(groups);
        StockGroupRegistry registry = new StockGroupRegistry(properties);
        registry.init();
        return registry;
    }

    @Test
    @DisplayName("설정된 그룹으로 종목의 샤드 그룹을 찾는다")
    void resolveGroup() {
        StockGroupRegistry registry = registry(Map.of(
                "A", List.of("005930", "000660"),
                "B", List.of("035420")));

        assertThat(registry.resolveGroup("005930")).isEqualTo("A");
        assertThat(registry.resolveGroup("000660")).isEqualTo("A");
        assertThat(registry.resolveGroup("035420")).isEqualTo("B");
    }

    @Test
    @DisplayName("설정에 없는 종목이면 400 ClientErrorException을 던진다 (서버 오류·Slack 알림 대상 아님)")
    void unknownStock_badRequest() {
        StockGroupRegistry registry = registry(Map.of("A", List.of("005930")));

        assertThatThrownBy(() -> registry.resolveGroup("999999"))
                .isInstanceOfSatisfying(ClientErrorException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).isEqualTo("지원하지 않는 종목입니다.");
                });
    }

    @Test
    @DisplayName("한 종목이 여러 그룹에 등록되어 있으면 나중 그룹으로 덮어쓴다")
    void duplicateMapping_lastWins() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("A", List.of("005930"));
        groups.put("B", List.of("005930"));

        assertThat(registry(groups).resolveGroup("005930")).isEqualTo("B");
    }
}
