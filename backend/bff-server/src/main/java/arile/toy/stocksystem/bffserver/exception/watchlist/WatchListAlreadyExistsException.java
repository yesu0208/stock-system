package arile.toy.stocksystem.bffserver.exception.watchlist;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class WatchListAlreadyExistsException extends ClientErrorException {
  public WatchListAlreadyExistsException(String stockCode) {
    super(HttpStatus.CONFLICT, "이미 관심종목에 추가된 종목입니다: " + stockCode);
  }
}
