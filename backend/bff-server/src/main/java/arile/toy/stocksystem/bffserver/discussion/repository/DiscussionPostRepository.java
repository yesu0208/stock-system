package arile.toy.stocksystem.bffserver.discussion.repository;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionPostRepository extends JpaRepository<DiscussionPostEntity, Long> {

    // 종목별 목록 (첫 페이지)
    List<DiscussionPostEntity> findByStockCodeOrderByPostIdDesc(String stockCode, Pageable pageable);

    // 종목별 목록 (커서 이후)
    List<DiscussionPostEntity> findByStockCodeAndPostIdLessThanOrderByPostIdDesc(
            String stockCode, Long cursor, Pageable pageable);

    // 내가 쓴 글 (첫 페이지)
    List<DiscussionPostEntity> findByAuthorIdOrderByPostIdDesc(String authorId, Pageable pageable);

    // 내가 쓴 글 (커서 이후)
    List<DiscussionPostEntity> findByAuthorIdAndPostIdLessThanOrderByPostIdDesc(
            String authorId, Long cursor, Pageable pageable);

    // 내가 댓글 단 글 (첫 페이지) - Comment 테이블과 조인
    @Query("""
            select distinct p from DiscussionPostEntity p
            join DiscussionCommentEntity c on c.postId = p.postId
            where c.authorId = :authorId
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findByCommentAuthor(@Param("authorId") String authorId, Pageable pageable);

    // 내가 댓글 단 글 (커서 이후)
    @Query("""
            select distinct p from DiscussionPostEntity p
            join DiscussionCommentEntity c on c.postId = p.postId
            where c.authorId = :authorId and p.postId < :cursor
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findByCommentAuthorAndCursor(
            @Param("authorId") String authorId, @Param("cursor") Long cursor, Pageable pageable);

    // 내가 스크랩한 글 (첫 페이지) - Scrap 테이블과 조인
    @Query("""
            select p from DiscussionPostEntity p
            join DiscussionScrapEntity s on s.postId = p.postId
            where s.userId = :userId
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findScrappedByUser(@Param("userId") String userId, Pageable pageable);

    // 내가 스크랩한 글 (커서 이후)
    @Query("""
            select p from DiscussionPostEntity p
            join DiscussionScrapEntity s on s.postId = p.postId
            where s.userId = :userId and p.postId < :cursor
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findScrappedByUserAndCursor(
            @Param("userId") String userId, @Param("cursor") Long cursor, Pageable pageable);
}
