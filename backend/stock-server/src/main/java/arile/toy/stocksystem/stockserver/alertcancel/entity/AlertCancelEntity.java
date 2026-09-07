package arile.toy.stocksystem.stockserver.alertcancel.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "alert_cancels")
public class AlertCancelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertCancelId;

    @Column(nullable = false)
    private Long alertId;

    @Column(nullable = false)
    private Instant cancelTime;

    public static AlertCancelEntity of(Long alertId) {
        var alertCancelEntity = new AlertCancelEntity();
        alertCancelEntity.setAlertId(alertId);
        alertCancelEntity.setCancelTime(Instant.now());
        return alertCancelEntity;
    }
}
