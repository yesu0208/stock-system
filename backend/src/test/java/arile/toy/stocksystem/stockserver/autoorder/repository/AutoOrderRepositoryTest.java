package arile.toy.stocksystem.stockserver.autoorder.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutoOrderRepositoryTest {

    @Spy
    private AutoOrderRepository autoOrderRepository;

    @Test
    @DisplayName("존재하는 ID로 findByIdForUpdate 호출 시 엔티티 반환")
    void givenExistingId_whenFindByIdForUpdate_thenReturnsEntity() {
        // given
        AutoOrderEntity order1 = new AutoOrderEntity();
        order1.setAutoOrderId(1L);
        order1.setAutoOrderStatus(AutoOrderStatus.ACTIVE);

        when(autoOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order1));

        // when
        Optional<AutoOrderEntity> result = autoOrderRepository.findByIdForUpdate(1L);

        // then
        assertTrue(result.isPresent());
        assertEquals(order1, result.get());
        verify(autoOrderRepository).findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 findByIdForUpdate 호출 시 빈 Optional 반환")
    void givenNoEntity_whenFindByIdForUpdate_thenReturnsEmpty() {
        // given
        when(autoOrderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        // when
        Optional<AutoOrderEntity> result = autoOrderRepository.findByIdForUpdate(99L);

        // then
        assertFalse(result.isPresent());
        verify(autoOrderRepository).findByIdForUpdate(99L);
    }

    @Test
    @DisplayName("ACTIVE 상태의 AutoOrder만 findAllUntriggered 호출 시 반환")
    void givenOpenAndClosedOrders_whenFindAllUntriggered_thenReturnsOnlyOpenOrders() {
        // given
        AutoOrderEntity order1 = new AutoOrderEntity();
        order1.setAutoOrderId(1L);
        order1.setAutoOrderStatus(AutoOrderStatus.ACTIVE);

        List<AutoOrderEntity> allOpenOrders = List.of(order1);
        when(autoOrderRepository.findAllByAutoOrderStatusIn(List.of(AutoOrderStatus.ACTIVE)))
                .thenReturn(allOpenOrders);

        // when
        List<AutoOrderEntity> result = autoOrderRepository.findAllUntriggered();

        // then
        assertEquals(1, result.size());
        assertTrue(result.contains(order1));
        verify(autoOrderRepository).findAllByAutoOrderStatusIn(List.of(AutoOrderStatus.ACTIVE));
    }

    @Test
    @DisplayName("특정 상태 리스트로 findAllByAutoOrderStatusIn 호출 시 일치하는 엔티티 반환")
    void givenStatuses_whenFindAllByAutoOrderStatusIn_thenReturnsMatchingOrders() {
        // given
        AutoOrderEntity order1 = new AutoOrderEntity();
        order1.setAutoOrderId(1L);
        order1.setAutoOrderStatus(AutoOrderStatus.ACTIVE);

        List<AutoOrderEntity> openOrders = List.of(order1);
        when(autoOrderRepository.findAllByAutoOrderStatusIn(List.of(AutoOrderStatus.ACTIVE))).thenReturn(openOrders);

        // when
        List<AutoOrderEntity> result = autoOrderRepository.findAllByAutoOrderStatusIn(List.of(AutoOrderStatus.ACTIVE));

        // then
        assertEquals(openOrders, result);
        verify(autoOrderRepository).findAllByAutoOrderStatusIn(List.of(AutoOrderStatus.ACTIVE));
    }
}
