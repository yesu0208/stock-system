package arile.toy.stocksystem.stockserver.market.holiday.repository;

import arile.toy.stocksystem.stockserver.market.holiday.entity.MarketHolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MarketHolidayRepository extends JpaRepository<MarketHolidayEntity, LocalDate> {
    List<MarketHolidayEntity> findAllByOrderByHolidayDateAsc();
    boolean existsByHolidayDate(LocalDate holidayDate);
}
