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
            entry("반도체", List.of("005930", "000660", "036930", "240810", "058470", "039030", "009150")),
            entry("2차전지", List.of("373220", "247540", "051910", "006400", "003670", "066970", "086520", "078600")),
            entry("바이오", List.of("207940", "068270", "196170", "145020", "214150", "141080")),
            entry("자동차", List.of("005380", "000270", "012330")),
            entry("IT/플랫폼", List.of("035420", "035720", "018260")),
            entry("철강/소재", List.of("005490", "010130")),
            entry("금융", List.of("105560", "055550", "000810", "032830", "086790")),
            entry("로봇", List.of("277810")),
            entry("게임", List.of("112040", "263750")),
            entry("가전/전자", List.of("066570")),
            entry("지주회사", List.of("034730")),
            entry("해운/운송", List.of("011200"))
    );
}
