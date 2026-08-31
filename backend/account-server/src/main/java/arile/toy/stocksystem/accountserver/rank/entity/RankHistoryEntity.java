package arile.toy.stocksystem.accountserver.rank.entity;

import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "rank_histories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "recordDate"}))
public class RankHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rankHistoryId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RankLevel rankLevel;

    @Column(nullable = false)
    private Long rp;

    @Column(nullable = false)
    private Long rpChange;

    public static RankHistoryEntity of(String username, LocalDate recordDate,
                                       RankLevel rankLevel, Long rp, Long rpChange) {
        var entity = new RankHistoryEntity();
        entity.setUsername(username);
        entity.setRecordDate(recordDate);
        entity.setRankLevel(rankLevel);
        entity.setRp(rp);
        entity.setRpChange(rpChange);
        return entity;
    }
}
