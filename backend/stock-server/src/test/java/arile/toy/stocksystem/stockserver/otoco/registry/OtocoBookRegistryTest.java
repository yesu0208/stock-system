package arile.toy.stocksystem.stockserver.otoco.registry;

import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Registry] OTOCO 진입·청산 북 테스트")
class OtocoBookRegistryTest {

    @DisplayName("진입 북: 종목별로 등록·덮어쓰기·제거한다")
    @Test
    void entryBook() {
        OtocoEntryBookRegistry sut = new OtocoEntryBookRegistry();
        OtocoDto first = OtocoFixtures.dto(1L, OtocoEntryDirection.BELOW, OtocoStatus.WAITING_ENTRY);
        OtocoDto second = OtocoFixtures.dto(2L, OtocoEntryDirection.ABOVE, OtocoStatus.WAITING_ENTRY);

        sut.register(first);
        sut.register(second);
        sut.register(first);
        assertThat(sut.getAll("005930")).containsExactlyInAnyOrder(first, second);
        assertThat(sut.getAll("000660")).isEmpty();

        sut.remove("005930", 1L);
        assertThat(sut.getAll("005930")).containsExactly(second);
    }

    @DisplayName("청산 북: 종목별로 등록·제거한다")
    @Test
    void exitBook() {
        OtocoExitBookRegistry sut = new OtocoExitBookRegistry();
        OtocoDto dto = OtocoFixtures.dto(OtocoStatus.WAITING_EXIT);

        sut.register(dto);
        assertThat(sut.getAll("005930")).containsExactly(dto);

        sut.remove("005930", 1L);
        sut.remove("005930", 99L);
        assertThat(sut.getAll("005930")).isEmpty();
    }
}
