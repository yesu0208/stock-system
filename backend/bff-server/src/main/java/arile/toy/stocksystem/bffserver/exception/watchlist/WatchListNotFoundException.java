package arile.toy.stocksystem.bffserver.exception.watchlist;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class WatchListNotFoundException extends ClientErrorException {
    public WatchListNotFoundException(String stockCode) {
        super(HttpStatus.NOT_FOUND, "관심종목에 존재하지 않는 종목입니다: " + stockCode);
    }
}
