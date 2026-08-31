package arile.toy.stocksystem.bffserver.discussion.repository;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionReactionEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.ReactionType;
import arile.toy.stocksystem.bffserver.discussion.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
