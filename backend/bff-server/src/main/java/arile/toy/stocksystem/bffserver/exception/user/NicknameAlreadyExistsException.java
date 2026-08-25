package arile.toy.stocksystem.bffserver.exception.user;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class NicknameAlreadyExistsException extends ClientErrorException {

    public NicknameAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Nickname already exists.");
    }

    public NicknameAlreadyExistsException(String nickname) {
        super(HttpStatus.CONFLICT, "Nickname " + nickname + " already exists.");
    }
}
