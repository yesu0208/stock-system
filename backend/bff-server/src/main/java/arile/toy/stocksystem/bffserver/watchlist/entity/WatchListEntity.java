package arile.toy.stocksystem.bffserver.watchlist.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "watch_lists",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "stockCode"}))
public class WatchListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long watchListId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    private String stockName;
    
    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Instant createdDateTime;

    public static WatchListEntity of(String username, String stockCode, String stockName, int sortOrder) {
        var entity = new WatchListEntity();
        entity.setUsername(username);
        entity.setStockCode(stockCode);
        entity.setStockName(stockName);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    @PrePersist
    private void prePersist() {
        this.createdDateTime = Instant.now();
    }
}
