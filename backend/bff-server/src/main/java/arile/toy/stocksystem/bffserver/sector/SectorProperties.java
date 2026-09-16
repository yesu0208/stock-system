package arile.toy.stocksystem.bffserver.sector;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@Component
@Getter
public class SectorProperties {

    /**
     * key: 업종명
     * value: 해당 업종에 속한 종목코드 리스트
     */
    private final Map<String, List<String>> groups = Map.ofEntries(
            entry("반도체", List.of("005930", "000660", "036930", "240810")),
            entry("2차전지", List.of("373220", "247540")),
            entry("바이오", List.of("207940", "068270", "196170")),
            entry("자동차", List.of("005380", "000270")),
            entry("IT/플랫폼", List.of("035420")),
            entry("철강/소재", List.of("005490")),
            entry("금융", List.of("105560")),
            entry("로봇", List.of("277810"))
    );
}