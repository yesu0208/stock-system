package arile.toy.stocksystem.bffserver.stockinfo.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 네이버 응답 값을 화면용 문자열로 바꾸는 private 순수 함수들의 입력·출력 표.
 * 공개 메서드로 모든 경우를 만들면 응답 JSON이 과도하게 늘어나 직접 호출로 검증.
 */
class NaverStockCrawlerFormatTest {

    private final NaverStockCrawlerClient client = new NaverStockCrawlerClient(mock(RestClient.class));

    private <T> T call(String method, Object... args) {
        return ReflectionTestUtils.invokeMethod(client, method, args);
    }

    // ===================== 코드 변환 =====================

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(value = {"1, UPPER_LIMIT", "2, UP", "3, STEADY", "4, LOWER_LIMIT", "5, DOWN", "9, UNKNOWN",
            "NULL, UNKNOWN"}, nullValues = "NULL")
    @DisplayName("숫자 등락 코드(1~5)를 방향으로 바꾸고, 모르는 코드·null은 UNKNOWN")
    void mapDirection(String code, String expected) {
        assertThat((String) call("mapDirection", code)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(value = {"상한가, UPPER_LIMIT", "상승, UP", "보합, STEADY", "하한가, LOWER_LIMIT", "하락, DOWN",
            "기타, UNKNOWN", "NULL, UNKNOWN"}, nullValues = "NULL")
    @DisplayName("한글 등락 문구를 방향으로 바꾸고, 모르는 문구·null은 UNKNOWN")
    void mapUpDownGbText(String text, String expected) {
        assertThat((String) call("mapUpDownGbText", text)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {"00, ''", "01, 투자주의", "02, 투자경고", "03, 투자위험", "99, ''", "NULL, ''"},
            nullValues = "NULL")
    @DisplayName("시장 경보 코드를 문구로 바꾸고, 정상(00)·모르는 코드·null은 빈 문자열")
    void mapWarningType(String code, String expected) {
        assertThat((String) call("mapWarningType", code)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {"0, ''", "NULL, ''", "1, 관리종목"}, nullValues = "NULL")
    @DisplayName("관리 상태 코드가 0이 아니면 관리종목")
    void mapManageStatus(String code, String expected) {
        assertThat((String) call("mapManageStatus", code)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"RISING, UP", "FALLING, DOWN", "EVEN, STEADY"})
    @DisplayName("지수 등락 이름을 방향으로 바꾸고, 그 외는 STEADY")
    void mapFluctuationsDirection(String name, String expected) {
        assertThat((String) call("mapFluctuationsDirection", name)).isEqualTo(expected);
    }

    // ===================== 숫자 포맷 =====================

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {"1234567, '1,234,567'", "-1500, '1,500'", "' 70000 ', '70,000'",
            "abc, abc", "NULL, ''"}, nullValues = "NULL")
    @DisplayName("formatComma: 절댓값에 천 단위 콤마, 숫자가 아니면 원문, null은 빈 문자열")
    void formatComma(String raw, String expected) {
        assertThat((String) call("formatComma", raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {"-1500, '-1,500'", "2000, '2,000'", "abc, abc", "NULL, ''"}, nullValues = "NULL")
    @DisplayName("formatSignedComma: 음수는 부호 유지, 숫자가 아니면 원문, null은 빈 문자열")
    void formatSignedComma(String raw, String expected) {
        assertThat((String) call("formatSignedComma", raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {"50.256, 50.26%", "abc, abc", "NULL, ''"}, nullValues = "NULL")
    @DisplayName("formatPercent: 소수 둘째 자리 반올림 후 %, 숫자가 아니면 원문, null은 빈 문자열")
    void formatPercent(String raw, String expected) {
        assertThat((String) call("formatPercent", raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {"1.43, +1.43%", "-1.5, -1.50%", "0, 0.00%", "abc, abc", "NULL, ''"}, nullValues = "NULL")
    @DisplayName("formatSignedRate: 양수만 + 부호, 숫자가 아니면 원문, null은 빈 문자열")
    void formatSignedRate(String raw, String expected) {
        assertThat((String) call("formatSignedRate", raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "종가 {0}, 대비 {1} → {2}")
    @CsvSource(value = {"71000, 1000, +1.43%", "69000, -1000, -1.43%", "1000, 1000, 0.00%",
            "abc, 1000, 0.00%", "NULL, 1000, 0.00%"}, nullValues = "NULL")
    @DisplayName("formatRate: 대비 ÷ 전일 종가, 전일 종가가 0이거나 숫자가 아니면 0.00%")
    void formatRate(String close, String change, String expected) {
        assertThat((String) call("formatRate", close, change)).isEqualTo(expected);
    }

    // ===================== 날짜 =====================

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource(value = {"20260924, 2026.09.24", "2026924, 2026924", "NULL, ''"}, nullValues = "NULL")
    @DisplayName("formatBizDate: 8자리만 yyyy.MM.dd로 바꾸고, 그 외는 원문, null은 빈 문자열")
    void formatBizDate(String raw, String expected) {
        assertThat((String) call("formatBizDate", raw)).isEqualTo(expected);
    }

    @Test
    @DisplayName("formatDealDateRange: 시작일과 종료일이 같으면 하루만 표시")
    void formatDealDateRange_sameDay() {
        assertThat((String) call("formatDealDateRange", "20260924", "20260924")).isEqualTo("2026.09.24");
    }

    // ===================== 숫자 파싱 =====================

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(value = {"'71,000', 71000", "' ', 0", "abc, 0", "NULL, 0"}, nullValues = "NULL")
    @DisplayName("parseLongOrZero: 콤마를 지우고 파싱, 비었거나 숫자가 아니면 0")
    void parseLongOrZero(String raw, long expected) {
        assertThat((long) call("parseLongOrZero", raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(value = {"' 100 ', 100", "abc, 0", "NULL, 0"}, nullValues = "NULL")
    @DisplayName("parseLongSafely: 숫자가 아니거나 null이면 0")
    void parseLongSafely(String raw, long expected) {
        assertThat((long) call("parseLongSafely", raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(value = {"' -1.5 ', -1.5", "abc, 0.0", "NULL, 0.0"}, nullValues = "NULL")
    @DisplayName("parseDoubleSafely: 숫자가 아니거나 null이면 0.0")
    void parseDoubleSafely(String raw, double expected) {
        assertThat((double) call("parseDoubleSafely", raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @CsvSource(value = {"NULL", "' '", "abc"}, nullValues = "NULL")
    @DisplayName("toBigDecimalSafely: 비었거나 숫자가 아니면 null")
    void toBigDecimalSafely_invalid(String raw) {
        assertThat((BigDecimal) call("toBigDecimalSafely", raw)).isNull();
    }

    @Test
    @DisplayName("toBigDecimalSafely: 앞뒤 공백을 지우고 숫자로 바꾼다")
    void toBigDecimalSafely_valid() {
        assertThat((BigDecimal) call("toBigDecimalSafely", " 71000 ")).isEqualByComparingTo("71000");
    }
}
