package arile.toy.stocksystem.bffserver.discussion.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "discussion_comments")
@EntityListeners(AuditingEntityListener.class)
public class DiscussionCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    @LastModifiedDate
    private Instant updatedDateTime;

    public static DiscussionCommentEntity of(Long postId, String authorId, String content) {
        var entity = new DiscussionCommentEntity();
        entity.setPostId(postId);
        entity.setAuthorId(authorId);
        entity.setContent(content);
        return entity;
    }

    public void edit(String content) {
        this.content = content;
    }

    @PrePersist
    private void prePersist() {
        this.createdDateTime = Instant.now();
        this.updatedDateTime = Instant.now();
    }
}
