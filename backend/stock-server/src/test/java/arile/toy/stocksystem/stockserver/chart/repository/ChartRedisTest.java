package arile.toy.stocksystem.stockserver.chart.repository;

import arile.toy.stocksystem.stockserver.chart.dto.CandleData;
import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.stockserver.chart.event.DailyCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.chart.event.MinuteCandleUpdateEvent;
import arile.toy.stocksystem.stockserver.chart.event.publisher.RedisDailyCandleEventPublisher;
import arile.toy.stocksystem.stockserver.chart.event.publisher.RedisMinuteCandleEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Redis] 차트 캐시 저장·캔들 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class ChartRedisTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private RedisTemplate<String, DailyCandleUpdateEvent> dailyTemplate;
    @Mock private RedisTemplate<String, MinuteCandleUpdateEvent> minuteTemplate;

    @DisplayName("일봉·분봉을 JSON으로 12시간 TTL과 함께 저장하고, 저장 실패는 삼킨다")
    @Test
    void whenSavingSnapshots_thenSetsJsonWithTtl() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        var sut = new ChartSnapshotRepository(stringRedisTemplate, new ObjectMapper());

        sut.saveDaily("005930", List.of(new CandleData("20260925", 1, 2, 3, 4, 5)));
        sut.saveMinute("005930", List.of(new MinuteCandle("20260925", "093000", 1, 2, 3, 4, 5)));

        then(valueOperations).should().set(eq("chart:daily:005930"), contains("\"date\":\"20260925\""), eq(Duration.ofHours(12)));
        then(valueOperations).should().set(eq("chart:minute:005930"), contains("\"time\":\"093000\""), eq(Duration.ofHours(12)));

        willThrow(new IllegalStateException("redis down")).given(valueOperations).set(anyString(), anyString(), any(Duration.class));
        assertThatNoException().isThrownBy(() -> sut.saveDaily("005930", List.of()));
    }

    @DisplayName("캔들 이벤트를 종목별 채널로 발행하고, 발행 실패는 삼킨다")
    @Test
    void whenPublishingCandles_thenSendsToChannels() {
        var daily = DailyCandleUpdateEvent.of("005930", new CandleData("20260925", 1, 2, 3, 4, 5));
        var minute = MinuteCandleUpdateEvent.of("005930", new MinuteCandle("20260925", "093000", 1, 2, 3, 4, 5));

        new RedisDailyCandleEventPublisher(dailyTemplate).publish(daily);
        new RedisMinuteCandleEventPublisher(minuteTemplate).publish(minute);

        then(dailyTemplate).should().convertAndSend("dailycandle.005930:event", daily);
        then(minuteTemplate).should().convertAndSend("minutecandle.005930:event", minute);

        given(dailyTemplate.convertAndSend(anyString(), any(DailyCandleUpdateEvent.class))).willThrow(new IllegalStateException());
        given(minuteTemplate.convertAndSend(anyString(), any(MinuteCandleUpdateEvent.class))).willThrow(new IllegalStateException());
        assertThatNoException().isThrownBy(() -> {
            new RedisDailyCandleEventPublisher(dailyTemplate).publish(daily);
            new RedisMinuteCandleEventPublisher(minuteTemplate).publish(minute);
        });
    }
}
