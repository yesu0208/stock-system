package arile.toy.stocksystem.stockserver.sharding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "stock-groups")
@Getter
@Setter
public class StockGroupProperties {
    private Map<String, List<String>> groups;
}