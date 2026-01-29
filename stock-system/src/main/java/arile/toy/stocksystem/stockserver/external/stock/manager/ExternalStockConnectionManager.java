package arile.toy.stocksystem.stockserver.external.stock.manager;

import arile.toy.stocksystem.stockserver.external.stock.approvalkey.ApprovalKeyService;
import arile.toy.stocksystem.stockserver.external.stock.listener.ExternalStockListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalStockConnectionManager {

    private final ExternalStockProperties stockProperties;

    private final ApprovalKeyService approvalKeyService;
    private final ExternalStockListener externalStockListener;

    @EventListener(ApplicationReadyEvent.class)
    public void connectAndSubscribe() {
        String approvalKey = approvalKeyService.issueApprovalKey();

        externalStockListener.connect(approvalKey);
        stockProperties.getStocks().forEach(externalStockListener::subscribe);
    }
}
