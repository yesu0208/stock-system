package arile.toy.stocksystem.stockserver.alertcancel.repository;

import arile.toy.stocksystem.stockserver.alertcancel.entity.AlertCancelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertCancelRepository extends JpaRepository<AlertCancelEntity, Long> {
}
