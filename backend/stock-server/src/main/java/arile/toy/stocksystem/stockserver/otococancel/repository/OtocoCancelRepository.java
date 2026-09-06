package arile.toy.stocksystem.stockserver.otococancel.repository;

import arile.toy.stocksystem.stockserver.otococancel.entity.OtocoCancelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtocoCancelRepository extends JpaRepository<OtocoCancelEntity, Long> {
}
