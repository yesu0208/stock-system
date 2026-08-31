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

    // 종목별 목록
    @Query("""
            select p from DiscussionPostEntity p
            where p.stockCode = :stockCode
            and (:cursor is null or p.postId < :cursor)
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findByStockCode(
            @Param("stockCode") String stockCode, @Param("cursor") Long cursor, Pageable pageable);

    // 내가 쓴 글
    @Query("""
            select p from DiscussionPostEntity p
            where p.authorId = :authorId
            and (:cursor is null or p.postId < :cursor)
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findByAuthorId(
            @Param("authorId") String authorId, @Param("cursor") Long cursor, Pageable pageable);

    // 내가 댓글 단 글
    @Query("""
            select distinct p from DiscussionPostEntity p
            join DiscussionCommentEntity c on c.postId = p.postId
            where c.authorId = :authorId
            and (:cursor is null or p.postId < :cursor)
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findByCommentAuthor(
            @Param("authorId") String authorId, @Param("cursor") Long cursor, Pageable pageable);

    // 내가 스크랩한 글
    @Query("""
            select p from DiscussionPostEntity p
            join DiscussionScrapEntity s on s.postId = p.postId
            where s.userId = :userId
            and (:cursor is null or p.postId < :cursor)
            order by p.postId desc
            """)
    List<DiscussionPostEntity> findScrappedByUser(
            @Param("userId") String userId, @Param("cursor") Long cursor, Pageable pageable);
}