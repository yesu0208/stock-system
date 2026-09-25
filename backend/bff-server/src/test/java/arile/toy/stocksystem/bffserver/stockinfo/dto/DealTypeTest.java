package arile.toy.stocksystem.bffserver.stockinfo.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DealTypeTest {

    @Test
    @DisplayName("매수·매도마다 비어 있지 않은 코드가 있고, 서로 다르다")
    void codes() {
        assertThat(DealType.values()).extracting(DealType::getCode)
                .allMatch(code -> code != null && !code.isBlank())
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("매수·매도 두 가지로 구성된다")
    void values() {
        assertThat(Arrays.stream(DealType.values()).map(Enum::name)).containsExactlyInAnyOrder("BUY", "SELL");
    }
}
