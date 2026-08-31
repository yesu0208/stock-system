package arile.toy.stocksystem.bffserver.discussion.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "discussion_scraps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"postId", "userId"}))
public class DiscussionScrapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scrapId;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private String userId;

    public static DiscussionScrapEntity of(Long postId, String userId) {
        var entity = new DiscussionScrapEntity();
        entity.setPostId(postId);
        entity.setUserId(userId);
        return entity;
    }
}
