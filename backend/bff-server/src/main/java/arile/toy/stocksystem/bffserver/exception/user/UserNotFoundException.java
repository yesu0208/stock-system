package arile.toy.stocksystem.bffserver.exception.user;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ClientErrorException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다.");
    }

    public UserNotFoundException(String username) {
        super(HttpStatus.NOT_FOUND, "아이디 '" + username + "'인 유저를 찾을 수 없습니다.");
    }
}
