package arile.toy.stocksystem.bffserver.discussion.repository;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionCommentRepository extends JpaRepository<DiscussionCommentEntity, Long> {

    List<DiscussionCommentEntity> findByPostIdOrderByCommentIdAsc(Long postId);

    long countByPostId(Long postId);
}
