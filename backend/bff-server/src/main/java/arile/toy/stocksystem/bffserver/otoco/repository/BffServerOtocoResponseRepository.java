package arile.toy.stocksystem.bffserver.otoco.repository;

import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponseMessage;

import java.util.List;

public interface BffServerOtocoResponseRepository {
    List<OtocoResponseMessage> findAll(String username);
}
