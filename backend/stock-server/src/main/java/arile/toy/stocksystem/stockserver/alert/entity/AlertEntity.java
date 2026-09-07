package arile.toy.stocksystem.stockserver.alert.entity;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.AlertStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertDirection direction;

    @Column(nullable = false)
    private Integer triggerPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @Column(nullable = false)
    private Instant registeredTime;

    public static AlertEntity of(String username, String stockCode, AlertDirection direction,
                                 Integer triggerPrice, AlertStatus status) {
        var alertEntity = new AlertEntity();
        alertEntity.setUsername(username);
        alertEntity.setStockCode(stockCode);
        alertEntity.setDirection(direction);
        alertEntity.setTriggerPrice(triggerPrice);
        alertEntity.setStatus(status);
        alertEntity.setRegisteredTime(Instant.now());
        return alertEntity;
    }

    public void changeStatus(AlertStatus status) {
        this.status = status;
    }
}
