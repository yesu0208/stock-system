package arile.toy.stocksystem.bffserver.sector;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SectorRegistry {

    public static final String UNCLASSIFIED_SECTOR = "기타/미분류";

    private final SectorProperties sectorProperties;

    private final Map<String, String> stockCodeToSector = new HashMap<>();

    @PostConstruct
    public void init() {
        if (sectorProperties.getGroups() == null) {
            log.warn("stock-sectors 설정이 비어있습니다. 모든 종목이 '{}'로 처리됩니다.", UNCLASSIFIED_SECTOR);
            return;
        }

        sectorProperties.getGroups().forEach((sector, stockCodes) ->
                stockCodes.forEach(stockCode -> {
                    String prev = stockCodeToSector.put(stockCode, sector);
                    if (prev != null) {
                        log.warn("종목코드 {}가 여러 업종({}, {})에 중복 매핑되어 있습니다. 마지막 값으로 덮어씁니다.",
                                stockCode, prev, sector);
                    }
                })
        );
    }

    public String resolveSector(String stockCode) {
        return stockCodeToSector.getOrDefault(stockCode, UNCLASSIFIED_SECTOR);
    }
}
