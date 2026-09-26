package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.dto.CandleData;
import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.stockserver.chart.event.DailyCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.chart.event.MinuteCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.chart.event.publisher.RedisDailyCandleEventPublisher;
import arile.toy.stocksystem.stockserver.chart.event.publisher.RedisMinuteCandleEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 실시간 일봉·분봉 생성 테스트")
@ExtendWith(MockitoExtension.class)
class LiveCandleServiceTest {

    private static final String TODAY_KST = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    @Mock private RedisDailyCandleEventPublisher dailyPublisher;
    @Mock private RedisMinuteCandleEventPublisher minutePublisher;

    @DisplayName("일봉: 틱의 시가·고가·저가·현재가·누적거래량으로 오늘(한국 날짜) 봉을 발행하고, 값이 없으면 발행하지 않는다")
    @Test
    void whenBuildingDaily_thenPublishesTodayCandle() {
        var sut = new LiveDailyCandleService(dailyPublisher);

        sut.buildAndPublish("005930", tick("093000", 70_000, 10));
        sut.buildAndPublish("005930", new TradePriceTickMessage(TickMessageType.TRADEPRICE, "005930", "093000",
                70_000, 0, 70_000, "0.00", null, 71_000, 68_500, 10, 1000, 0L, 0, 0, "1", 0));

        then(dailyPublisher).should().publish(DailyCandleUpdateEvent.of("005930",
                new CandleData(TODAY_KST, 69_000, 71_000, 68_500, 70_000, 1000)));
        then(dailyPublisher).shouldHaveNoMoreInteractions();
    }

    @DisplayName("일봉: 시가·고가·저가·현재가·누적거래량 중 하나라도 없으면 발행하지 않는다")
    @ParameterizedTest(name = "{0} 없음")
    @ValueSource(strings = {"startPrice", "highPrice", "lowPrice", "curPrice", "totalVolume"})
    void givenMissingField_whenBuildingDaily_thenSkips(String missing) {
        var sut = new LiveDailyCandleService(dailyPublisher);

        sut.buildAndPublish("005930", new TradePriceTickMessage(TickMessageType.TRADEPRICE, "005930", "093000",
                missing.equals("curPrice") ? null : 70_000, 0, 70_000, "0.00",
                missing.equals("startPrice") ? null : 69_000,
                missing.equals("highPrice") ? null : 71_000,
                missing.equals("lowPrice") ? null : 68_500,
                10,
                missing.equals("totalVolume") ? null : 1000,
                0L, 0, 0, "1", 0));

        then(dailyPublisher).shouldHaveNoInteractions();
    }

    @DisplayName("분봉: 같은 분의 틱은 고가·저가·종가·거래량을 누적하고, 분이 바뀌면 새 봉을 시작한다")
    @Test
    void whenUpdatingMinute_thenAccumulatesPerMinute() {
        var sut = new LiveMinuteCandleService(minutePublisher);

        sut.updateAndPublish("005930", tick("093001", 70_000, 10));
        sut.updateAndPublish("005930", tick("093030", 70_500, 5));
        sut.updateAndPublish("005930", tick("093059", 69_800, 3));
        sut.updateAndPublish("005930", tick("093100", 70_100, 7));

        ArgumentCaptor<MinuteCandleUpdateEvent> captor = ArgumentCaptor.forClass(MinuteCandleUpdateEvent.class);
        then(minutePublisher).should(times(4)).publish(captor.capture());
        List<MinuteCandle> candles = captor.getAllValues().stream().map(MinuteCandleUpdateEvent::candle).toList();
        assertThat(candles.get(2)).isEqualTo(new MinuteCandle(TODAY_KST, "093000", 70_000, 70_500, 69_800, 69_800, 18));
        assertThat(candles.get(3)).isEqualTo(new MinuteCandle(TODAY_KST, "093100", 70_100, 70_100, 70_100, 70_100, 7));
    }

    @DisplayName("분봉: 현재가·체결시각이 없거나 시각이 짧으면 발행하지 않는다")
    @Test
    void givenInvalidTick_whenUpdatingMinute_thenSkips() {
        var sut = new LiveMinuteCandleService(minutePublisher);

        sut.updateAndPublish("005930", tick(null, 70_000, 10));
        sut.updateAndPublish("005930", tick("093", 70_000, 10));
        sut.updateAndPublish("005930", new TradePriceTickMessage(TickMessageType.TRADEPRICE, "005930", "093000",
                null, 0, 70_000, "0.00", 69_000, 71_000, 68_500, 10, 1000, 0L, 0, 0, "1", 0));

        then(minutePublisher).shouldHaveNoInteractions();
    }

    @DisplayName("분봉: 체결량이 없으면 0으로 보고 봉을 만든다")
    @Test
    void givenNullVolumeTick_whenUpdatingMinute_thenTreatsAsZero() {
        var sut = new LiveMinuteCandleService(minutePublisher);

        sut.updateAndPublish("005930", new TradePriceTickMessage(TickMessageType.TRADEPRICE, "005930", "093000",
                70_000, 0, 70_000, "0.00", 69_000, 71_000, 68_500, null, 1000, 0L, 0, 0, "1", 0));

        then(minutePublisher).should().publish(MinuteCandleUpdateEvent.of("005930",
                new MinuteCandle(TODAY_KST, "093000", 70_000, 70_000, 70_000, 70_000, 0)));
    }

    private TradePriceTickMessage tick(String time, int price, int volumeTick) {
        return new TradePriceTickMessage(TickMessageType.TRADEPRICE, "005930", time, price, 0, price, "0.00",
                69_000, 71_000, 68_500, volumeTick, 1000, 0L, 0, 0, "1", 0);
    }
}
