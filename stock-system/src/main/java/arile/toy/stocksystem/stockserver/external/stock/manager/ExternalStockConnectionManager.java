package arile.toy.stocksystem.stockserver.external.stock.manager;

import arile.toy.stocksystem.stockserver.external.stock.approvalkey.ApprovalKeyService;
import arile.toy.stocksystem.stockserver.external.stock.listener.ExternalStockListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class ExternalStockConnectionManager {

    private final List<String> stockCodes =
            List.of("247540", "005930"); // application.yaml로 이동

    private final ApprovalKeyService approvalKeyService;
    private final ExternalStockListener externalStockListener;

    @EventListener(ApplicationReadyEvent.class)
    public void connectAndSubscribe() {
        String approvalKey = approvalKeyService.issueApprovalKey();

        externalStockListener.connect(approvalKey);
        stockCodes.forEach(externalStockListener::subscribe);
    }
}
