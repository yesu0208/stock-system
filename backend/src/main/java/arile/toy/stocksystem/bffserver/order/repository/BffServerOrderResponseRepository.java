package arile.toy.stocksystem.bffserver.order.repository;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;

import java.util.List;

public interface BffServerOrderResponseRepository {
    List<OrderResponseMessage> findAll(String username);
}
