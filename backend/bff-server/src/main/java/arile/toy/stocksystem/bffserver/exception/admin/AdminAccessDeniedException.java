package arile.toy.stocksystem.bffserver.exception.admin;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class AdminAccessDeniedException extends ClientErrorException {
    public AdminAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "다른 유저의 정보를 조회할 권한이 없습니다.");
    }
}
