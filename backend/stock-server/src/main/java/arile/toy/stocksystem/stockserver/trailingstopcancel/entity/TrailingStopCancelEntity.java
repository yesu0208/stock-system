package arile.toy.stocksystem.stockserver.trailingstopcancel.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "trailing_stop_cancels")
public class TrailingStopCancelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trailingStopCancelId;

    @Column(nullable = false)
    private Long trailingStopId;

    @Column(nullable = false)
    private Instant cancelTime;

    public static TrailingStopCancelEntity of(Long trailingStopId) {
        var entity = new TrailingStopCancelEntity();
        entity.setTrailingStopId(trailingStopId);
        entity.setCancelTime(Instant.now());
        return entity;
    }
}
