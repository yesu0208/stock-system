package arile.toy.stocksystem.stockserver.market.holiday.service;

import arile.toy.stocksystem.stockserver.exception.market.PastMarketHolidayDateException;
import arile.toy.stocksystem.stockserver.market.holiday.entity.MarketHolidayEntity;
import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRedisPublisher;
import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 휴장일 관리 테스트")
@ExtendWith(MockitoExtension.class)
class MarketHolidayServiceTest {

    private static final LocalDate TODAY_KST = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @InjectMocks private MarketHolidayService sut;

    @Mock private MarketHolidayRepository marketHolidayRepository;
    @Mock private MarketHolidayRedisPublisher marketHolidayRedisPublisher;

    @DisplayName("휴장일 목록을 날짜 오름차순으로 조회한다")
    @Test
    void whenGettingAll_thenReturnsSortedHolidays() {
        var holidays = List.of(MarketHolidayEntity.of(TODAY_KST.plusDays(1), "임시"),
                MarketHolidayEntity.of(TODAY_KST.plusDays(10), "추석"));
        given(marketHolidayRepository.findAllByOrderByHolidayDateAsc()).willReturn(holidays);

        assertThat(sut.getAll()).isEqualTo(holidays);
    }

    @DisplayName("미래 날짜의 새 휴장일은 저장하고 Redis에도 반영한다")
    @Test
    void givenNewFutureDate_whenAdding_thenSavesAndPublishes() {
        LocalDate tomorrow = TODAY_KST.plusDays(1);
        given(marketHolidayRepository.findById(tomorrow)).willReturn(Optional.empty());
        given(marketHolidayRepository.save(any(MarketHolidayEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MarketHolidayEntity result = sut.addHoliday(tomorrow, "임시 휴장");

        assertThat(result.getHolidayDate()).isEqualTo(tomorrow);
        assertThat(result.getMemo()).isEqualTo("임시 휴장");
        then(marketHolidayRedisPublisher).should().add(tomorrow);
    }

    @DisplayName("이미 등록된 날짜면 새로 저장하지 않고 기존 휴장일을 반환하며, Redis에는 다시 반영한다")
    @Test
    void givenExistingDate_whenAdding_thenReturnsExistingAndRepublishes() {
        LocalDate date = TODAY_KST.plusDays(3);
        MarketHolidayEntity existing = MarketHolidayEntity.of(date, "기존 메모");
        given(marketHolidayRepository.findById(date)).willReturn(Optional.of(existing));

        MarketHolidayEntity result = sut.addHoliday(date, "새 메모");

        assertThat(result).isSameAs(existing);
        assertThat(result.getMemo()).isEqualTo("기존 메모");
        then(marketHolidayRepository).should(never()).save(any());
        then(marketHolidayRedisPublisher).should().add(date);
    }

    @DisplayName("오늘(KST) 또는 과거 날짜는 등록할 수 없다")
    @Test
    void givenTodayOrPast_whenAdding_thenThrows() {
        assertThatThrownBy(() -> sut.addHoliday(TODAY_KST, "당일"))
                .isInstanceOf(PastMarketHolidayDateException.class);
        assertThatThrownBy(() -> sut.addHoliday(TODAY_KST.minusDays(1), "어제"))
                .isInstanceOf(PastMarketHolidayDateException.class);

        then(marketHolidayRepository).shouldHaveNoInteractions();
        then(marketHolidayRedisPublisher).shouldHaveNoInteractions();
    }

    @DisplayName("휴장일을 삭제하고 Redis에서도 제거한다")
    @Test
    void givenDate_whenRemoving_thenDeletesAndPublishes() {
        LocalDate date = TODAY_KST.plusDays(3);

        sut.removeHoliday(date);

        then(marketHolidayRepository).should().deleteById(date);
        then(marketHolidayRedisPublisher).should().remove(date);
    }
}
