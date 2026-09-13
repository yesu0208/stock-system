package arile.toy.stocksystem.bffserver.exception.user;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends ClientErrorException {

    public PasswordMismatchException() {
        super(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
    }
}
