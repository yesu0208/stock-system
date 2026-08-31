package arile.toy.stocksystem.bffserver.exception.discussion;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class DiscussionCommentNotFoundException extends ClientErrorException {
    public DiscussionCommentNotFoundException(Long commentId) {
        super(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다: " + commentId);
    }
}
