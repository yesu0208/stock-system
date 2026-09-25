package arile.toy.stocksystem.bffserver.stockinfo.event;

import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StockInfoRedisPublishersTest {

    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
    }

    @Test
    @DisplayName("국내 지수는 market:main:event 채널로 JSON을 발행한다")
    void marketMain() throws Exception {
        MarketMainResponse response = new MarketMainResponse(null, null, null);

        new MarketMainRedisPublisher(redisTemplate, new ObjectMapper()).publish(response);

        verify(redisTemplate).convertAndSend("market:main:event", new ObjectMapper().writeValueAsString(response));
    }

    @Test
    @DisplayName("환율은 market:global:event 채널로 JSON을 발행한다")
    void globalMarket() throws Exception {
        GlobalMarketResponse response = new GlobalMarketResponse(List.of());

        new GlobalMarketRedisPublisher(redisTemplate, new ObjectMapper()).publish(response);

        verify(redisTemplate).convertAndSend("market:global:event", new ObjectMapper().writeValueAsString(response));
    }

    @Test
    @DisplayName("종목 상세는 종목별 채널(stockdetail.{종목}:event)로 발행한다 (실시간 구독 관리자가 구독하는 이름과 같아야 함)")
    void stockDetail() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        StockDetailTickMessage message = mock(StockDetailTickMessage.class);
        given(message.stockCode()).willReturn("005930");
        given(objectMapper.writeValueAsString(message)).willReturn("{\"stockCode\":\"005930\"}");

        new StockDetailRedisPublisher(redisTemplate, objectMapper).publish(message);

        verify(redisTemplate).convertAndSend("stockdetail.005930:event", "{\"stockCode\":\"005930\"}");
        assertThat(StockDetailRedisPublisher.channel("005930")).isEqualTo("stockdetail.005930:event");
    }

    @Test
    @DisplayName("JSON 변환에 실패해도 예외를 던지지 않고 발행하지 않는다")
    void serializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        given(objectMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("boom") {});

        assertThatCode(() -> new MarketMainRedisPublisher(redisTemplate, objectMapper)
                .publish(new MarketMainResponse(null, null, null))).doesNotThrowAnyException();

        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("종목 상세: JSON 변환에 실패해도 예외를 던지지 않고 발행하지 않는다")
    void stockDetail_serializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        StockDetailTickMessage message = mock(StockDetailTickMessage.class);
        given(message.stockCode()).willReturn("005930");
        given(objectMapper.writeValueAsString(message)).willThrow(new JsonProcessingException("boom") {});

        assertThatCode(() -> new StockDetailRedisPublisher(redisTemplate, objectMapper).publish(message))
                .doesNotThrowAnyException();

        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("환율: JSON 변환에 실패해도 예외를 던지지 않고 발행하지 않는다")
    void globalMarket_serializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        given(objectMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("boom") {});

        assertThatCode(() -> new GlobalMarketRedisPublisher(redisTemplate, objectMapper)
                .publish(new GlobalMarketResponse(List.of()))).doesNotThrowAnyException();

        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
