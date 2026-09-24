package arile.toy.stocksystem.bffserver.sector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SectorPropertiesTest {

    private final Map<String, List<String>> groups = new SectorProperties().getGroups();

    @Test
    @DisplayName("한 종목은 하나의 업종에만 속한다 (중복이면 포트폴리오 업종 비중이 두 번 합산됨)")
    void eachStockInOneSector() {
        List<String> allCodes = groups.values().stream().flatMap(List::stream).toList();

        assertThat(allCodes).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("종목코드는 모두 6자리 숫자다")
    void stockCodesAreSixDigits() {
        assertThat(groups.values().stream().flatMap(List::stream))
                .allMatch(code -> code.matches("\\d{6}"));
    }

    @Test
    @DisplayName("모든 업종에 종목이 하나 이상 있다")
    void noEmptySector() {
        assertThat(groups.values()).allMatch(codes -> !codes.isEmpty());
    }

    @Test
    @DisplayName("대표 종목이 알맞은 업종에 속한다")
    void knownMappings() {
        assertThat(groups.get("반도체")).contains("005930", "000660");
        assertThat(groups.get("2차전지")).contains("373220");
        assertThat(groups.get("자동차")).contains("005380");
        assertThat(groups).hasSize(8);
    }
}
