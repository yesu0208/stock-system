package arile.toy.stocksystem.bffserver.trailingstop.repository;

import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;

import java.util.List;

public interface BffServerTrailingStopResponseRepository {
    List<TrailingStopResponseMessage> findAll(String username);
}
