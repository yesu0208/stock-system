package arile.toy.stocksystem.bffserver.exception.close;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class MarketClosedException extends ClientErrorException {

    public MarketClosedException() {
        super(HttpStatus.BAD_REQUEST, "장 종료된 종목입니다.");
    }

    public MarketClosedException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
