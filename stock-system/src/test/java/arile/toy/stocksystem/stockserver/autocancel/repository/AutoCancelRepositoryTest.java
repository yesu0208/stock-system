package arile.toy.stocksystem.stockserver.autocancel.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import arile.toy.stocksystem.stockserver.autocancel.entity.AutoCancelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutoCancelRepositoryTest {

    @Mock
    private AutoCancelRepository autoCancelRepository;

    private AutoCancelEntity entity1;
    private AutoCancelEntity entity2;

    @BeforeEach
    void setUp() {
        entity1 = new AutoCancelEntity();
        entity1.setAutoCancelId(1L);
        entity1.setAutoOrderId(3L);

        entity2 = new AutoCancelEntity();
        entity2.setAutoCancelId(2L);
        entity2.setAutoOrderId(4L);
    }

    @Test
    @DisplayName("모든 AutoCancel 엔티티 조회 시 리스트 반환")
    void givenEntities_whenFindAll_thenReturnsList() {
        // given
        when(autoCancelRepository.findAll()).thenReturn(List.of(entity1, entity2));

        // when
        List<AutoCancelEntity> result = autoCancelRepository.findAll();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(entity1));
        assertTrue(result.contains(entity2));
        verify(autoCancelRepository).findAll();
    }

    @Test
    @DisplayName("존재하는 ID로 조회 시 AutoCancel 엔티티 반환")
    void givenEntityId_whenFindById_thenReturnsEntity() {
        // given
        when(autoCancelRepository.findById(1L)).thenReturn(Optional.of(entity1));

        // when
        Optional<AutoCancelEntity> result = autoCancelRepository.findById(1L);

        // then
        assertTrue(result.isPresent());
        assertEquals(entity1, result.get());
        verify(autoCancelRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
    void givenNonExistingId_whenFindById_thenReturnsEmpty() {
        // given
        when(autoCancelRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        Optional<AutoCancelEntity> result = autoCancelRepository.findById(999L);

        // then
        assertFalse(result.isPresent());
        verify(autoCancelRepository).findById(999L);
    }

    @Test
    @DisplayName("엔티티 저장 시 save 호출 후 저장된 엔티티 반환")
    void givenEntity_whenSave_thenCallsSave() {
        // given
        when(autoCancelRepository.save(entity1)).thenReturn(entity1);

        // when
        AutoCancelEntity result = autoCancelRepository.save(entity1);

        // then
        assertEquals(entity1, result);
        verify(autoCancelRepository).save(entity1);
    }

    @Test
    @DisplayName("ID로 삭제 시 deleteById 호출")
    void givenEntityId_whenDeleteById_thenCallsDelete() {
        // when
        autoCancelRepository.deleteById(1L);

        // then
        verify(autoCancelRepository).deleteById(1L);
    }
}
