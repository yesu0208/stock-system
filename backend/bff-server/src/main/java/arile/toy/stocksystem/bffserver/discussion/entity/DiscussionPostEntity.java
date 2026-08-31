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
@Table(name = "discussion_posts")
@EntityListeners(AuditingEntityListener.class)
public class DiscussionPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    private String stockName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    @LastModifiedDate
    private Instant updatedDateTime;

    public static DiscussionPostEntity of(String stockCode, String stockName, String title,
                                          String authorId, String content) {
        var entity = new DiscussionPostEntity();
        entity.setStockCode(stockCode);
        entity.setStockName(stockName);
        entity.setTitle(title);
        entity.setAuthorId(authorId);
        entity.setContent(content);
        return entity;
    }

    public void edit(String title, String content) {
        this.title = title;
        this.content = content;
    }

    @PrePersist
    private void prePersist() {
        this.createdDateTime = Instant.now();
        this.updatedDateTime = Instant.now();
    }
}
