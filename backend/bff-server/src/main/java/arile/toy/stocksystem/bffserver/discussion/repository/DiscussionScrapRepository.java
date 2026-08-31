package arile.toy.stocksystem.bffserver.discussion.repository;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionScrapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscussionScrapRepository extends JpaRepository<DiscussionScrapEntity, Long> {

    Optional<DiscussionScrapEntity> findByPostIdAndUserId(Long postId, String userId);

    boolean existsByPostIdAndUserId(Long postId, String userId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("""
            select s.postId as postId, count(s) as cnt
            from DiscussionScrapEntity s
            where s.postId in :postIds
            group by s.postId
            """)
    List<ScrapCountRow> countGroupByPostIds(@Param("postIds") List<Long> postIds);

    interface ScrapCountRow {
        Long getPostId();
        long getCnt();
    }
}
