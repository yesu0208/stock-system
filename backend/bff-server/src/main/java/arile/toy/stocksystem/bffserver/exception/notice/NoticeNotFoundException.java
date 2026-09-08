package arile.toy.stocksystem.bffserver.exception.notice;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class NoticeNotFoundException extends ClientErrorException {
    public NoticeNotFoundException(Long noticeId) {
        super(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다: " + noticeId);
    }
}
