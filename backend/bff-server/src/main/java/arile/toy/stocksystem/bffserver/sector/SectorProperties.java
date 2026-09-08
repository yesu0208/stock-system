package arile.toy.stocksystem.bffserver.sector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "stock-sectors")
@Getter
@Setter
public class SectorProperties {
    /**
     * key: 업종명
     * value: 해당 업종에 속한 종목코드 리스트
     */
    private Map<String, List<String>> groups;
}
