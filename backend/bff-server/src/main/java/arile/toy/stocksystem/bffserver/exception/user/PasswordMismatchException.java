package arile.toy.stocksystem.bffserver.exception.user;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends ClientErrorException {

    public PasswordMismatchException() {
        super(HttpStatus.BAD_REQUEST, "Current password does not match.");
    }
}
