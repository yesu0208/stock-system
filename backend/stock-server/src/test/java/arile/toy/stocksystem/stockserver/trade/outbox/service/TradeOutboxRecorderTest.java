package arile.toy.stocksystem.stockserver.trade.outbox.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.stockserver.trade.outbox.entity.OutboxStatus;
import arile.toy.stocksystem.stockserver.trade.outbox.entity.TradeOutboxEntity;
import arile.toy.stocksystem.stockserver.trade.outbox.repository.TradeOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Outbox] 체결 이벤트 outbox 기록 테스트")
@ExtendWith(MockitoExtension.class)
class TradeOutboxRecorderTest {

    @InjectMocks private TradeOutboxRecorder sut;

    @Mock private TradeOutboxRepository tradeOutboxRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("체결 이벤트를 JSON으로 직렬화해 TRADE_EXECUTED 타입의 PENDING outbox로 저장한다")
    @Test
    void givenEvent_whenRecording_thenSavesPendingOutbox() throws Exception {
        // Given
        var event = event();

        // When
        sut.record(event);

        // Then
        ArgumentCaptor<TradeOutboxEntity> captor = ArgumentCaptor.forClass(TradeOutboxEntity.class);
        then(tradeOutboxRepository).should().save(captor.capture());

        TradeOutboxEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("TRADE_EXECUTED");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(objectMapper.readValue(saved.getPayload(), TradeExecutedEvent.class)).isEqualTo(event);
    }

    @DisplayName("직렬화에 실패하면 예외를 던져 체결 트랜잭션이 롤백되게 한다")
    @Test
    void givenSerializationFails_whenRecording_thenThrows() throws Exception {
        willThrow(new JsonProcessingException("boom") {}).given(objectMapper).writeValueAsString(any());

        assertThatThrownBy(() -> sut.record(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Outbox 기록 실패");

        then(tradeOutboxRepository).shouldHaveNoInteractions();
    }

    @DisplayName("저장에 실패해도 예외를 던져 체결 트랜잭션이 롤백되게 한다")
    @Test
    void givenSaveFails_whenRecording_thenThrows() {
        given(tradeOutboxRepository.save(any(TradeOutboxEntity.class)))
                .willThrow(new IllegalStateException("db error"));

        assertThatThrownBy(() -> sut.record(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Outbox 기록 실패")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    private TradeExecutedEvent event() {
        return TradeExecutedEvent.of(100L, 1L, "user", "005930", TradeType.BUY, LeverageRatio.X2,
                70_000, 70_000, 4, 42L, 140_000L);
    }
}
