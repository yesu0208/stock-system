package arile.toy.stocksystem.stockserver.cancel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import arile.toy.stocksystem.stockserver.cancel.entity.CancelEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;


@DataJpaTest
class CancelRepositoryTest {

    @Autowired
    private CancelRepository cancelRepository;

    @Test
    @DisplayName("Cancel 엔티티 저장 후 findById 호출 시 동일 엔티티 반환")
    void givenCancelEntity_whenSave_thenFindByIdReturnsSame() {
        // given
        CancelEntity entity = new CancelEntity();
        entity.setOrderId(1L);
        entity.setCancelTime(Instant.now());

        // when
        CancelEntity saved = cancelRepository.save(entity);
        Optional<CancelEntity> found = cancelRepository.findById(saved.getCancelId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("저장된 Cancel 엔티티 삭제 후 findById 호출 시 존재하지 않음")
    void givenSavedEntity_whenDelete_thenNoLongerExists() {
        // given
        CancelEntity entity = new CancelEntity();
        entity.setOrderId(1L);
        entity.setCancelTime(Instant.now());
        CancelEntity saved = cancelRepository.save(entity);

        // when
        cancelRepository.delete(saved);

        // then
        Optional<CancelEntity> found = cancelRepository.findById(saved.getCancelId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("엔티티가 없을 때 findAll 호출 시 빈 리스트 반환")
    void givenNoEntity_whenFindAll_thenEmptyList() {
        // given
        cancelRepository.deleteAll();

        // when
        var all = cancelRepository.findAll();

        // then
        assertThat(all).isEmpty();
    }
}
