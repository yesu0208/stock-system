package arile.toy.stocksystem.bffserver.exception.user;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class NicknameAlreadyExistsException extends ClientErrorException {

    public NicknameAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
    }

}
