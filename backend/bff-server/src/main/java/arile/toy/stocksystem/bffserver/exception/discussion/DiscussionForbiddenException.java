package arile.toy.stocksystem.bffserver.exception.discussion;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class DiscussionForbiddenException extends ClientErrorException {
  public DiscussionForbiddenException() {
    super(HttpStatus.FORBIDDEN, "작성자 본인만 수정/삭제할 수 있습니다.");
  }
}
