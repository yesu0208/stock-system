package arile.toy.stocksystem.stockserver.trailingstop.registry;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Registry] 트레일링 스탑 북 레지스트리 테스트")
class TrailingStopBookRegistryTest {

    private final TrailingStopBookRegistry sut = new TrailingStopBookRegistry();

    @DisplayName("종목별로 트레일링 스탑을 등록·조회하고, 등록되지 않은 종목은 비어 있다")
    @Test
    void givenRegistered_whenGettingAll_thenReturnsByStock() {
        sut.register(dto(1L, "005930", 70_000));
        sut.register(dto(2L, "000660", 70_000));

        assertThat(sut.getAll("005930")).extracting(TrailingStopDto::trailingStopId).containsExactly(1L);
        assertThat(sut.getAll("999999")).isEmpty();
    }

    @DisplayName("같은 ID로 갱신하면 기존 값을 덮어쓴다")
    @Test
    void givenUpdated_whenGettingAll_thenReplaced() {
        sut.register(dto(1L, "005930", 70_000));

        sut.update(dto(1L, "005930", 72_000));

        assertThat(sut.getAll("005930")).singleElement()
                .extracting(TrailingStopDto::basePrice).isEqualTo(72_000);
    }

    @DisplayName("ID로 제거하면 해당 종목에서만 사라진다")
    @Test
    void givenRemoved_whenGettingAll_thenGoneOnlyInThatStock() {
        sut.register(dto(1L, "005930", 70_000));
        sut.register(dto(1L, "000660", 70_000));

        sut.remove("005930", 1L);

        assertThat(sut.getAll("005930")).isEmpty();
        assertThat(sut.getAll("000660")).hasSize(1);
    }

    private TrailingStopDto dto(Long id, String stockCode, int basePrice) {
        return new TrailingStopDto(id, "user", stockCode, TrailingStopType.SELL, LeverageRatio.SPOT,
                10, 3.0, basePrice, 67_900, 67_900, TrailingStopStatus.ACTIVE,
                Instant.parse("2026-09-25T00:00:00Z"));
    }
}
