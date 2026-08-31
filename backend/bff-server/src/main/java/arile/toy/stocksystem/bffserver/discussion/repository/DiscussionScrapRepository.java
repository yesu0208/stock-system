package arile.toy.stocksystem.bffserver.discussion.repository;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionScrapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscussionScrapRepository extends JpaRepository<DiscussionScrapEntity, Long> {

    Optional<DiscussionScrapEntity> findByPostIdAndUserId(Long postId, String userId);

    boolean existsByPostIdAndUserId(Long postId, String userId);

    long countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, String userId);
}
