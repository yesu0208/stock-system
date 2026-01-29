package arile.toy.stocksystem.stockserver.external.stock.manager;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "external")
@Getter
@Setter
public class ExternalStockProperties {

    private List<String> stocks;
}
