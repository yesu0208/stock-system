package arile.toy.stocksystem.bffserver.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClientErrorException extends RuntimeException {
    public final HttpStatus status;

    public ClientErrorException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

}
