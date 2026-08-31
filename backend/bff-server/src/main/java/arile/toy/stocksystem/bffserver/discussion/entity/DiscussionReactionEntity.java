package arile.toy.stocksystem.bffserver.discussion.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "discussion_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"targetType", "targetId", "userId"}))
public class DiscussionReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reactionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

    public static DiscussionReactionEntity of(TargetType targetType, Long targetId,
                                              String userId, ReactionType reactionType) {
        var entity = new DiscussionReactionEntity();
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setUserId(userId);
        entity.setReactionType(reactionType);
        return entity;
    }

    public void changeReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }
}
