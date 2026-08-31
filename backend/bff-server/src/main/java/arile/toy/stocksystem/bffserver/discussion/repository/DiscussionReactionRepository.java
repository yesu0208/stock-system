package arile.toy.stocksystem.bffserver.discussion.repository;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionReactionEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.ReactionType;
import arile.toy.stocksystem.bffserver.discussion.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscussionReactionRepository extends JpaRepository<DiscussionReactionEntity, Long> {

    Optional<DiscussionReactionEntity> findByTargetTypeAndTargetIdAndUserId(
            TargetType targetType, Long targetId, String userId);

    long countByTargetTypeAndTargetIdAndReactionType(
            TargetType targetType, Long targetId, ReactionType reactionType);

    void deleteByTargetTypeAndTargetIdAndUserId(
            TargetType targetType, Long targetId, String userId);

    void deleteByTargetTypeAndTargetIdIn(TargetType targetType, List<Long> targetIds);

    @Query("""
            select r.targetId as targetId, r.reactionType as reactionType, count(r) as cnt
            from DiscussionReactionEntity r
            where r.targetType = :targetType and r.targetId in :targetIds
            group by r.targetId, r.reactionType
            """)
    List<ReactionCountRow> countGroupByTargetIds(
            @Param("targetType") TargetType targetType, @Param("targetIds") List<Long> targetIds);

    interface ReactionCountRow {
        Long getTargetId();
        ReactionType getReactionType();
        long getCnt();
    }
}

