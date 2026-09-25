package arile.toy.stocksystem.stockserver.market.holiday.service;

import arile.toy.stocksystem.stockserver.exception.market.PastMarketHolidayDateException;
import arile.toy.stocksystem.stockserver.market.holiday.entity.MarketHolidayEntity;
import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRedisPublisher;
import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketHolidayService {

    // 휴장일 판정(MarketTimeChecker)과 같은 기준으로 "오늘"을 계산 (서버 기본 시간대가 UTC여도 KST 기준 유지)
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MarketHolidayRepository marketHolidayRepository;
    private final MarketHolidayRedisPublisher marketHolidayRedisPublisher;

    public List<MarketHolidayEntity> getAll() {
        return marketHolidayRepository.findAllByOrderByHolidayDateAsc();
    }

    @Transactional
    public MarketHolidayEntity addHoliday(LocalDate date, String memo) {
        if (!date.isAfter(LocalDate.now(KST))) {
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
