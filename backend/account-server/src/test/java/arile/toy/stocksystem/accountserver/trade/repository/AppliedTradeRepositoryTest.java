package arile.toy.stocksystem.accountserver.trade.repository;

import arile.toy.stocksystem.accountserver.trade.entity.AppliedTradeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("[Repository] 반영 완료 체결 기록 저장소 테스트")
@DataJpaTest
class AppliedTradeRepositoryTest {

    @Autowired private AppliedTradeRepository sut;

    @DisplayName("저장한 체결(종목코드 + 체결 ID)은 존재한다고 판단한다")
    @Test
    void givenSaved_whenCheckingExists_thenTrue() {
        sut.saveAndFlush(AppliedTradeEntity.of("005930", 100L));

        assertThat(sut.existsByStockCodeAndTradeId("005930", 100L)).isTrue();
    }

    @DisplayName("종목코드 또는 체결 ID 중 하나라도 다르면 존재하지 않는다고 판단한다")
    @Test
    void givenDifferentKey_whenCheckingExists_thenFalse() {
        sut.saveAndFlush(AppliedTradeEntity.of("005930", 100L));

        assertThat(sut.existsByStockCodeAndTradeId("000660", 100L)).isFalse();
        assertThat(sut.existsByStockCodeAndTradeId("005930", 101L)).isFalse();
    }

    @DisplayName("같은 체결 ID라도 종목코드가 다르면 별개의 체결로 저장할 수 있다")
    @Test
    void givenSameTradeIdDifferentStock_whenSaving_thenBothSaved() {
        sut.saveAndFlush(AppliedTradeEntity.of("005930", 100L));
        sut.saveAndFlush(AppliedTradeEntity.of("000660", 100L));

        assertThat(sut.count()).isEqualTo(2L);
    }

    @DisplayName("같은 체결(종목코드 + 체결 ID)을 다시 저장하면 유니크 제약 위반으로 실패한다")
    @Test
    void givenDuplicate_whenSaving_thenThrowsDataIntegrityViolation() {
        sut.saveAndFlush(AppliedTradeEntity.of("005930", 100L));

        assertThatThrownBy(() -> sut.saveAndFlush(AppliedTradeEntity.of("005930", 100L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
