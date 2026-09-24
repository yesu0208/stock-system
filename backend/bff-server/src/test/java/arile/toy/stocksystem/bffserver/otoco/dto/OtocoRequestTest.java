package arile.toy.stocksystem.bffserver.otoco.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OtocoRequestTest {

    private static final int ENTRY = 70_000;

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static OtocoRequest request(OtocoExitMode tpMode, Integer tpPrice, Double tpPct,
                                        OtocoExitMode slMode, Integer slPrice, Double slPct) {
        return new OtocoRequest("005930", OtocoEntryDirection.ABOVE, 10, ENTRY,
                tpMode, tpPrice, tpPct, slMode, slPrice, slPct, null);
    }

    /** 손절은 정상값으로 고정하고 익절만 바꾼 요청 */
    private static OtocoRequest withTp(OtocoExitMode mode, Integer price, Double pct) {
        return request(mode, price, pct, OtocoExitMode.PCT, null, 3.0);
    }

    /** 익절은 정상값으로 고정하고 손절만 바꾼 요청 */
    private static OtocoRequest withSl(OtocoExitMode mode, Integer price, Double pct) {
        return request(OtocoExitMode.PCT, null, 5.0, mode, price, pct);
    }

    private static Set<String> violatedProperties(OtocoRequest request) {
        Set<ConstraintViolation<OtocoRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("레버리지 배율이 없으면 SPOT으로, 있으면 그대로 사용한다")
    void leverageRatioOrDefault() {
        assertThat(withTp(OtocoExitMode.PCT, null, 5.0).leverageRatioOrDefault()).isEqualTo(LeverageRatio.SPOT);
        assertThat(new OtocoRequest("005930", OtocoEntryDirection.BELOW, 10, ENTRY,
                OtocoExitMode.PCT, null, 5.0, OtocoExitMode.PCT, null, 3.0, LeverageRatio.X2)
                .leverageRatioOrDefault()).isEqualTo(LeverageRatio.X2);
    }

    @Test
    @DisplayName("익절·손절이 모두 올바르면 위반이 없다")
    void valid() {
        assertThat(violatedProperties(request(OtocoExitMode.PRICE, 75_000, null, OtocoExitMode.PRICE, 65_000, null)))
                .isEmpty();
    }

    // ===================== 익절 =====================

    @Nested
    @DisplayName("익절(TP)")
    class TakeProfit {

        @Test
        @DisplayName("PRICE: 진입가보다 1원이라도 높으면 허용한다")
        void price_aboveEntry() {
            assertThat(violatedProperties(withTp(OtocoExitMode.PRICE, ENTRY + 1, null))).isEmpty();
        }

        @ParameterizedTest(name = "tpPrice={0}")
        @ValueSource(ints = {ENTRY, ENTRY - 1, 0, -1_000})
        @DisplayName("PRICE: 진입가 이하면 위반이다")
        void price_notAboveEntry(int tpPrice) {
            assertThat(violatedProperties(withTp(OtocoExitMode.PRICE, tpPrice, null))).containsExactly("tpValid");
        }

        @Test
        @DisplayName("PRICE: 가격이 없으면 위반이다 (퍼센트만 있어도)")
        void price_missing() {
            assertThat(violatedProperties(withTp(OtocoExitMode.PRICE, null, 5.0))).containsExactly("tpValid");
        }

        @ParameterizedTest(name = "tpPct={0}")
        @ValueSource(doubles = {0.1, 5.0, 30.0})
        @DisplayName("PCT: 0.1%~30% 사이면 허용한다")
        void pct_inRange(double tpPct) {
            assertThat(violatedProperties(withTp(OtocoExitMode.PCT, null, tpPct))).isEmpty();
        }

        @ParameterizedTest(name = "tpPct={0}")
        @ValueSource(doubles = {0.09, 30.1, 50.0, -5.0})
        @DisplayName("PCT: 0.1% 미만이거나 30% 초과면 위반이다")
        void pct_outOfRange(double tpPct) {
            assertThat(violatedProperties(withTp(OtocoExitMode.PCT, null, tpPct))).containsExactly("tpValid");
        }

        @ParameterizedTest(name = "tpPct=[{0}]")
        @NullSource
        @DisplayName("PCT: 퍼센트가 없으면 위반이다 (가격만 있어도)")
        void pct_missing(Double tpPct) {
            assertThat(violatedProperties(withTp(OtocoExitMode.PCT, 75_000, tpPct))).containsExactly("tpValid");
        }
    }

    // ===================== 손절 =====================

    @Nested
    @DisplayName("손절(SL)")
    class StopLoss {

        @ParameterizedTest(name = "slPrice={0}")
        @ValueSource(ints = {1, ENTRY - 1})
        @DisplayName("PRICE: 0원보다 크고 진입가보다 낮으면 허용한다")
        void price_inRange(int slPrice) {
            assertThat(violatedProperties(withSl(OtocoExitMode.PRICE, slPrice, null))).isEmpty();
        }

        @ParameterizedTest(name = "slPrice={0}")
        @ValueSource(ints = {ENTRY, ENTRY + 1})
        @DisplayName("PRICE: 진입가 이상이면 위반이다")
        void price_notBelowEntry(int slPrice) {
            assertThat(violatedProperties(withSl(OtocoExitMode.PRICE, slPrice, null))).containsExactly("slValid");
        }

        @ParameterizedTest(name = "slPrice={0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("PRICE: 0원 이하면 위반이다 (영원히 발동하지 않는 손절 방지)")
        void price_notPositive(int slPrice) {
            assertThat(violatedProperties(withSl(OtocoExitMode.PRICE, slPrice, null))).containsExactly("slValid");
        }

        @Test
        @DisplayName("PRICE: 가격이 없으면 위반이다")
        void price_missing() {
            assertThat(violatedProperties(withSl(OtocoExitMode.PRICE, null, 3.0))).containsExactly("slValid");
        }

        @ParameterizedTest(name = "slPct={0}")
        @ValueSource(doubles = {0.1, 30.0})
        @DisplayName("PCT: 0.1%~30% 사이면 허용한다")
        void pct_inRange(double slPct) {
            assertThat(violatedProperties(withSl(OtocoExitMode.PCT, null, slPct))).isEmpty();
        }

        @ParameterizedTest(name = "slPct={0}")
        @ValueSource(doubles = {0.09, 30.1, 100.0})
        @DisplayName("PCT: 0.1% 미만이거나 30% 초과면 위반이다")
        void pct_outOfRange(double slPct) {
            assertThat(violatedProperties(withSl(OtocoExitMode.PCT, null, slPct))).containsExactly("slValid");
        }

        @Test
        @DisplayName("PCT: 퍼센트가 없으면 위반이다")
        void pct_missing() {
            assertThat(violatedProperties(withSl(OtocoExitMode.PCT, 65_000, null))).containsExactly("slValid");
        }
    }

    // ===================== 필수값 누락 =====================

    @Test
    @DisplayName("모드나 진입가가 없으면 @NotNull만 위반으로 보고하고, 익절·손절 검증은 중복 보고하지 않는다")
    void missingModeOrEntry_reportedOnlyByNotNull() {
        OtocoRequest noModes = request(null, null, null, null, null, null);
        OtocoRequest noEntry = new OtocoRequest("005930", OtocoEntryDirection.ABOVE, 10, null,
                OtocoExitMode.PRICE, 75_000, null, OtocoExitMode.PRICE, 65_000, null, null);

        assertThat(violatedProperties(noModes)).containsExactlyInAnyOrder("tpMode", "slMode");
        assertThat(violatedProperties(noEntry)).containsExactly("entryTriggerPrice");
    }
}
