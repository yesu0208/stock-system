package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] OTOCO 진입 주문 체결·취소 훅 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoEntryFillServiceTest {

    @InjectMocks private OtocoEntryFillService sut;

    @Mock private OtocoRepository otocoRepository;
    @Mock private OtocoExitBookRegistry otocoExitBookRegistry;
    @Mock private OtocoResponseEventPublisher otocoResponseEventPublisher;
    @Mock private StockServerOtocoResponseRepository stockServerOtocoResponseRepository;

    @DisplayName("OTOCO와 무관한 주문이거나 ENTRY_ORDER_PLACED가 아니면 아무것도 하지 않는다")
    @Test
    void givenUnrelatedOrClosed_whenHooked_thenNothing() {
        given(otocoRepository.findByEntryOrderIdForUpdate(100L)).willReturn(Optional.empty());
        given(otocoRepository.findByEntryOrderIdForUpdate(200L))
                .willReturn(Optional.of(OtocoFixtures.entity(OtocoStatus.CANCELED)));

        sut.onOrderFilled(100L);
        sut.onOrderPartiallyFilled(100L, 3);
        sut.onOrderCanceled(100L);
        sut.onOrderFilled(200L);
        sut.onOrderPartiallyFilled(200L, 3);
        sut.onOrderCanceled(200L);

        then(otocoRepository).should(never()).save(any());
        then(otocoExitBookRegistry).shouldHaveNoInteractions();
        then(stockServerOtocoResponseRepository).shouldHaveNoInteractions();
        then(otocoResponseEventPublisher).shouldHaveNoInteractions();
    }

    @Nested
    @DisplayName("완전체결")
    class Filled {

        @DisplayName("주식 예약 없이 WAITING_EXIT로 저장하고 청산 북 등록·응답 갱신·발행한다")
        @Test
        void givenPlaced_whenFilled_thenWaitingExit() {
            OtocoEntity entity = givenPlaced();

            sut.onOrderFilled(100L);

            assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.WAITING_EXIT);
            then(otocoRepository).should().save(entity);
            then(otocoExitBookRegistry).should().register(argThat(d ->
                    d.otocoId().equals(1L) && d.otocoStatus() == OtocoStatus.WAITING_EXIT));
            then(stockServerOtocoResponseRepository).should().update(eq("user"), eq(1L),
                    argThat(m -> m.entryRemainingQuantity() == null));
            then(otocoResponseEventPublisher).should().publishEntryFilled(any());
        }

        @DisplayName("응답 갱신이 실패해도 예외 없이 발행한다 (체결 롤백 방지)")
        @Test
        void givenUpdateFails_whenFilled_thenDoesNotThrow() {
            givenPlaced();
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerOtocoResponseRepository).update(eq("user"), eq(1L), any());

            sut.onOrderFilled(100L);

            then(otocoResponseEventPublisher).should().publishEntryFilled(any());
        }
    }

    @Nested
    @DisplayName("부분체결")
    class PartiallyFilled {

        @DisplayName("상태는 그대로 두고 잔량을 담아 응답 갱신·발행한다")
        @Test
        void givenPlaced_whenPartiallyFilled_thenUpdatesRemaining() {
            OtocoEntity entity = givenPlaced();

            sut.onOrderPartiallyFilled(100L, 4);

            assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.ENTRY_ORDER_PLACED);
            then(stockServerOtocoResponseRepository).should().update(eq("user"), eq(1L),
                    argThat(m -> m.entryRemainingQuantity() == 4));
            then(otocoResponseEventPublisher).should().publishEntryPartiallyFilled(argThat(d -> d.entryRemainingQuantity() == 4));
        }

        @DisplayName("응답 갱신이 실패해도 예외 없이 발행한다")
        @Test
        void givenUpdateFails_whenPartiallyFilled_thenDoesNotThrow() {
            givenPlaced();
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerOtocoResponseRepository).update(eq("user"), eq(1L), any());

            sut.onOrderPartiallyFilled(100L, 4);

            then(otocoResponseEventPublisher).should().publishEntryPartiallyFilled(any());
        }
    }

    @Nested
    @DisplayName("취소")
    class Canceled {

        @DisplayName("CANCELED로 저장하고 응답 삭제·발행한다")
        @Test
        void givenPlaced_whenCanceled_thenCanceled() {
            OtocoEntity entity = givenPlaced();

            sut.onOrderCanceled(100L);

            assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
            then(otocoRepository).should().save(entity);
            then(stockServerOtocoResponseRepository).should().delete("user", 1L);
            then(otocoResponseEventPublisher).should().publishEntryCanceled(any());
        }

        @DisplayName("응답 삭제가 실패해도 예외 없이 발행한다")
        @Test
        void givenDeleteFails_whenCanceled_thenDoesNotThrow() {
            givenPlaced();
            willThrow(new IllegalStateException("redis down")).given(stockServerOtocoResponseRepository).delete("user", 1L);

            sut.onOrderCanceled(100L);

            then(otocoResponseEventPublisher).should().publishEntryCanceled(any());
        }
    }

    private OtocoEntity givenPlaced() {
        OtocoEntity entity = OtocoFixtures.entity(OtocoStatus.ENTRY_ORDER_PLACED);
        entity.setEntryOrderId(100L);
        given(otocoRepository.findByEntryOrderIdForUpdate(100L)).willReturn(Optional.of(entity));
        return entity;
    }
}
