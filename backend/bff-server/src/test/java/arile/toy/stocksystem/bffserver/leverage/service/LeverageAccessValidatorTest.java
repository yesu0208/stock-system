package arile.toy.stocksystem.bffserver.leverage.service;

import arile.toy.stocksystem.bffserver.exception.leverage.LeverageNotAllowedException;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.rank.client.RankApiClient;
import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageAccessValidatorTest {

    @Mock
    private RankApiClient rankApiClient;

    @InjectMocks
    private LeverageAccessValidator validator;

    private static RankResponse rank(String tier) {
        return new RankResponse("user1", tier, 1, 1000L, tier, 900L, 1100L);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = LeverageRatio.class, names = {"SPOT", "X1_5"})
    @DisplayName("등급 제한이 없는 배율은 등급을 조회하지 않고 통과한다 (등급 API 장애가 현물 거래에 영향 없음)")
    void noRequiredTier_skipsLookup(LeverageRatio ratio) {
        assertThatCode(() -> validator.validate("user1", ratio)).doesNotThrowAnyException();

        verifyNoInteractions(rankApiClient);
    }

    @ParameterizedTest(name = "{0} 배율, {1} 등급 → 허용")
    @CsvSource({"X2, GOLD", "X2, PLATINUM", "X2, DIAMOND", "X2_5, PLATINUM", "X2_5, DIAMOND"})
    @DisplayName("필요 등급 이상이면 통과한다")
    void enoughTier_allowed(LeverageRatio ratio, String tier) {
        given(rankApiClient.getRank("user1")).willReturn(rank(tier));

        assertThatCode(() -> validator.validate("user1", ratio)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} 배율, {1} 등급 → 거부 (필요: {2})")
    @CsvSource({"X2, SILVER, GOLD", "X2, BRONZE, GOLD", "X2_5, GOLD, PLATINUM"})
    @DisplayName("필요 등급보다 낮으면 필요 등급을 담아 거부한다")
    void insufficientTier_rejected(LeverageRatio ratio, String tier, String requiredTier) {
        given(rankApiClient.getRank("user1")).willReturn(rank(tier));

        assertThatThrownBy(() -> validator.validate("user1", ratio))
                .isInstanceOf(LeverageNotAllowedException.class)
                .hasMessageContaining(requiredTier);
    }

    @Test
    @DisplayName("등급을 알 수 없는 값(언랭 등)이면 가장 낮은 등급으로 보고 거부한다")
    void unknownTier_rejected() {
        given(rankApiClient.getRank("user1")).willReturn(rank("UNKNOWN_TIER"));

        assertThatThrownBy(() -> validator.validate("user1", LeverageRatio.X2))
                .isInstanceOf(LeverageNotAllowedException.class);
    }

    @Test
    @DisplayName("[fail-closed] 등급 조회가 실패하면(null) 레버리지를 거부한다")
    void rankLookupFails_rejected() {
        given(rankApiClient.getRank("user1")).willReturn(null);

        assertThatThrownBy(() -> validator.validate("user1", LeverageRatio.X2))
                .isInstanceOf(LeverageNotAllowedException.class);
    }
}
