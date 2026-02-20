package arile.toy.stocksystem.stockserver.autocancel.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "auto_cancels")
public class AutoCancelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long autoCancelId;

    @Column(nullable = false)
    private Long autoOrderId;

    @Column(nullable = false)
    private Instant cancelTime;

    public static AutoCancelEntity of(Long autoOrderId) {
        var autoCancelEntity = new AutoCancelEntity();
        autoCancelEntity.setAutoOrderId(autoOrderId);
        autoCancelEntity.setCancelTime(Instant.now());
        return autoCancelEntity;
    }
}
