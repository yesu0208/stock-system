package arile.toy.stocksystem.stockserver.trade.outbox.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "trade_outbox_events")
public class TradeOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outboxId;

    @Column(nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(nullable = false)
    private Instant createdDateTime;

    private Instant publishedDateTime;

    public static TradeOutboxEntity of(String eventType, String payload) {
        var entity = new TradeOutboxEntity();
        entity.setEventType(eventType);
        entity.setPayload(payload);
        entity.setStatus(OutboxStatus.PENDING);
        return entity;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedDateTime = Instant.now();
    }

    @PrePersist
    private void prePersist() {
        this.createdDateTime = Instant.now();
    }
}
