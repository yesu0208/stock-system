package arile.toy.stocksystem.bffserver.alert.repository;

import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;

import java.util.List;

public interface BffServerAlertResponseRepository {
    List<AlertResponseMessage> findAll(String username);
}
