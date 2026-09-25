package arile.toy.stocksystem.stockserver.trade.outbox.service;

import arile.toy.stocksystem.stockserver.trade.outbox.entity.OutboxStatus;
import arile.toy.stocksystem.stockserver.trade.outbox.entity.TradeOutboxEntity;
import arile.toy.stocksystem.stockserver.trade.outbox.repository.TradeOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Outbox] 체결 이벤트 outbox 발행 테스트")
@ExtendWith(MockitoExtension.class)
class TradeOutboxPublisherTest {

    private static final String STREAM_KEY = "trade-executed";

    @InjectMocks private TradeOutboxPublisher sut;

    @Mock private TradeOutboxRepository tradeOutboxRepository;
    @Mock private RedisTemplate<String, Object> streamRedisTemplate;
    @Mock private StreamOperations<String, Object, Object> streamOps;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "streamKey", STREAM_KEY);
        lenient().doReturn(streamOps).when(streamRedisTemplate).opsForStream();
    }

    @DisplayName("대기 중인 outbox가 없으면 발행·저장을 하지 않는다")
    @Test
    void givenNoPending_whenPublishing_thenDoesNothing() {
        given(tradeOutboxRepository.findTop100ByStatusOrderByOutboxIdAsc(OutboxStatus.PENDING))
                .willReturn(List.of());

        sut.publishPending();

        then(streamOps).shouldHaveNoInteractions();
        then(tradeOutboxRepository).should(never()).saveAll(any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @DisplayName("대기 중인 outbox를 순서대로 스트림에 발행하고 PUBLISHED로 저장한다")
    @Test
    void givenPending_whenPublishing_thenPublishesInOrderAndMarksPublished() {
        // Given
        TradeOutboxEntity first = outbox(1L, "{\"tradeId\":100}");
        TradeOutboxEntity second = outbox(2L, "{\"tradeId\":101}");
        List<TradeOutboxEntity> pending = List.of(first, second);
        given(tradeOutboxRepository.findTop100ByStatusOrderByOutboxIdAsc(OutboxStatus.PENDING)).willReturn(pending);
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        // When
        sut.publishPending();

        // Then
        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        then(streamOps).should(times(2)).add(captor.capture());

        List<MapRecord> sent = captor.getAllValues();
        assertThat(sent).extracting(MapRecord::getStream).containsOnly(STREAM_KEY);
        assertThat((Map<String, Object>) sent.get(0).getValue())
                .containsEntry("type", "TRADE_EXECUTED")
                .containsEntry("payload", "{\"tradeId\":100}");
        assertThat((Map<String, Object>) sent.get(1).getValue())
                .containsEntry("payload", "{\"tradeId\":101}");

        assertThat(first.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(first.getPublishedDateTime()).isNotNull();
        assertThat(second.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        then(tradeOutboxRepository).should().saveAll(pending);
    }

    @DisplayName("발행에 실패하면 그 건과 이후 건은 PENDING으로 두고 발행을 멈춘다 (순서 보장)")
    @Test
    void givenPublishFailsInMiddle_whenPublishing_thenStopsAndKeepsRestPending() {
        // Given
        TradeOutboxEntity first = outbox(1L, "p1");
        TradeOutboxEntity second = outbox(2L, "p2");
        TradeOutboxEntity third = outbox(3L, "p3");
        List<TradeOutboxEntity> pending = List.of(first, second, third);
        given(tradeOutboxRepository.findTop100ByStatusOrderByOutboxIdAsc(OutboxStatus.PENDING)).willReturn(pending);
        given(streamOps.add(any(MapRecord.class)))
                .willReturn(RecordId.of("1-0"))
                .willThrow(new IllegalStateException("redis down"));

        // When
        sut.publishPending();

        // Then
        then(streamOps).should(times(2)).add(any(MapRecord.class));
        assertThat(first.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(second.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(third.getStatus()).isEqualTo(OutboxStatus.PENDING);
        then(tradeOutboxRepository).should().saveAll(pending);
    }

    private TradeOutboxEntity outbox(Long id, String payload) {
        TradeOutboxEntity entity = TradeOutboxEntity.of("TRADE_EXECUTED", payload);
        entity.setOutboxId(id);
        return entity;
    }
}
