package arile.toy.stocksystem.stockserver.otococancel.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "otoco_cancels")
public class OtocoCancelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long otocoCancelId;

    @Column(nullable = false)
    private Long otocoId;

    @Column(nullable = false)
    private Instant cancelTime;

    public static OtocoCancelEntity of(Long otocoId) {
        var entity = new OtocoCancelEntity();
        entity.setOtocoId(otocoId);
        entity.setCancelTime(Instant.now());
        return entity;
    }
}
