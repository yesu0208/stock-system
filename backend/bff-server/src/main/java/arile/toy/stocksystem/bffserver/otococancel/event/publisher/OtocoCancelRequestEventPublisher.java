package arile.toy.stocksystem.bffserver.otococancel.event.publisher;

import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelRequestEvent;

public interface OtocoCancelRequestEventPublisher {
    void publishOtocoCancel(OtocoCancelRequestEvent event);
}
