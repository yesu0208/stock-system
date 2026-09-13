package arile.toy.stocksystem.bffserver.exception.user;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ClientErrorException {

    public UserAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "이미 가입된 유저입니다.");
    }

    public UserAlreadyExistsException(String username) {
        super(HttpStatus.CONFLICT, "아이디 '" + username + "'는 이미 사용 중입니다.");
    }
}
