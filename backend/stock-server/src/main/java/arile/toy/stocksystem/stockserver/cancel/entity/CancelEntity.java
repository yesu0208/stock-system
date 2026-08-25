package arile.toy.stocksystem.stockserver.cancel.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "cancels")
public class CancelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cancelId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Instant cancelTime;

    public static CancelEntity of(Long orderId) {
        var cancelEntity = new CancelEntity();
        cancelEntity.setOrderId(orderId);
        cancelEntity.setCancelTime(Instant.now());
        return cancelEntity;
    }
}
