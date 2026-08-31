package arile.toy.stocksystem.bffserver.discussion.repository;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionCommentRepository extends JpaRepository<DiscussionCommentEntity, Long> {

    List<DiscussionCommentEntity> findByPostIdOrderByCommentIdAsc(Long postId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("""
            select c.postId as postId, count(c) as cnt
            from DiscussionCommentEntity c
            where c.postId in :postIds
            group by c.postId
            """)
    List<CommentCountRow> countGroupByPostIds(@Param("postIds") List<Long> postIds);

    interface CommentCountRow {
        Long getPostId();
        long getCnt();
    }
}
