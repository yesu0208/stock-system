package arile.toy.stocksystem.stockserver.market.holiday.service;

import arile.toy.stocksystem.stockserver.exception.market.PastMarketHolidayDateException;
import arile.toy.stocksystem.stockserver.market.holiday.entity.MarketHolidayEntity;
import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRedisPublisher;
import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketHolidayService {

    private final MarketHolidayRepository marketHolidayRepository;
    private final MarketHolidayRedisPublisher marketHolidayRedisPublisher;

    public List<MarketHolidayEntity> getAll() {
        return marketHolidayRepository.findAllByOrderByHolidayDateAsc();
    }

    @Transactional
    public MarketHolidayEntity addHoliday(LocalDate date, String memo) {
        if (!date.isAfter(LocalDate.now())) {
            throw new PastMarketHolidayDateException();
        }

        MarketHolidayEntity entity = marketHolidayRepository.findById(date)
                .orElseGet(() -> marketHolidayRepository.save(MarketHolidayEntity.of(date, memo)));

        marketHolidayRedisPublisher.add(date);
        return entity;
    }

    @Transactional
    public void removeHoliday(LocalDate date) {
        marketHolidayRepository.deleteById(date);
        marketHolidayRedisPublisher.remove(date);
    }
}
