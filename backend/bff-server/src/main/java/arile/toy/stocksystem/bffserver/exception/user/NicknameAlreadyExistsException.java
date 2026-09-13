package arile.toy.stocksystem.bffserver.exception.user;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class NicknameAlreadyExistsException extends ClientErrorException {

    public NicknameAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
    }

    public NicknameAlreadyExistsException(String nickname) {
        super(HttpStatus.CONFLICT, "닉네임 '" + nickname + "'은(는) 이미 사용 중입니다.");
    }
}
