package arile.toy.stocksystem.bffserver.exception.admin;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class AdminUserAccountNotFoundException extends ClientErrorException {
    public AdminUserAccountNotFoundException(String username) {
        super(HttpStatus.NOT_FOUND, "계좌 정보를 찾을 수 없습니다: " + username);
    }
}
