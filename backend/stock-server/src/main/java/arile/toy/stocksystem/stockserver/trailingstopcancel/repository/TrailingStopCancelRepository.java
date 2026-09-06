package arile.toy.stocksystem.stockserver.trailingstopcancel.repository;

import arile.toy.stocksystem.stockserver.trailingstopcancel.entity.TrailingStopCancelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrailingStopCancelRepository extends JpaRepository<TrailingStopCancelEntity, Long> {
}
