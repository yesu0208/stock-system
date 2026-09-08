package arile.toy.stocksystem.bffserver.notice.entity;

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
@Table(name = "notices")
@EntityListeners(AuditingEntityListener.class)
public class NoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    @LastModifiedDate
    private Instant updatedDateTime;

    public static NoticeEntity of(String title, String content, String authorId) {
        var entity = new NoticeEntity();
        entity.setTitle(title);
        entity.setContent(content);
        entity.setAuthorId(authorId);
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
