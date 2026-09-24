package arile.toy.stocksystem.bffserver.sector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SectorRegistryTest {

    private static SectorRegistry registry(Map<String, List<String>> groups) {
        SectorProperties properties = mock(SectorProperties.class);
        given(properties.getGroups()).willReturn(groups);
        SectorRegistry registry = new SectorRegistry(properties);
        registry.init();
        return registry;
    }

    @Test
    @DisplayName("설정된 업종으로 종목을 분류하고, 설정에 없는 종목은 기타/미분류로 분류한다")
    void resolveSector() {
        SectorRegistry registry = registry(Map.of(
                "반도체", List.of("005930", "000660"),
                "인터넷", List.of("035420")));

        assertThat(registry.resolveSector("005930")).isEqualTo("반도체");
        assertThat(registry.resolveSector("035420")).isEqualTo("인터넷");
        assertThat(registry.resolveSector("999999")).isEqualTo(SectorRegistry.UNCLASSIFIED_SECTOR);
    }

    @Test
    @DisplayName("업종 설정이 비어 있으면 모든 종목을 기타/미분류로 분류한다")
    void emptySettings() {
        SectorRegistry registry = registry(null);

        assertThat(registry.resolveSector("005930")).isEqualTo(SectorRegistry.UNCLASSIFIED_SECTOR);
    }

    @Test
    @DisplayName("한 종목이 여러 업종에 등록되어 있으면 나중 업종으로 덮어쓴다")
    void duplicateMapping_lastWins() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("반도체", List.of("005930"));
        groups.put("전자", List.of("005930"));

        assertThat(registry(groups).resolveSector("005930")).isEqualTo("전자");
    }
}
