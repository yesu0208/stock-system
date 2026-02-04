package arile.toy.stocksystem.bffserver.autoorder.repository;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;

import java.util.List;

public interface BffServerAutoOrderResponseRepository {
    List<AutoOrderResponseMessage> findAll(String username);
}
