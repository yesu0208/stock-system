package arile.toy.stocksystem.stockserver.cancel.repository;

import arile.toy.stocksystem.stockserver.cancel.entity.CancelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CancelRepository extends JpaRepository<CancelEntity, Long> {
}
