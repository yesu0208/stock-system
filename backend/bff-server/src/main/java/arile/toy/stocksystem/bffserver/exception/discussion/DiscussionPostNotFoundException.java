package arile.toy.stocksystem.bffserver.exception.discussion;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.springframework.http.HttpStatus;

public class DiscussionPostNotFoundException extends ClientErrorException {
    public DiscussionPostNotFoundException(Long postId) {
        super(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: " + postId);
    }
}
