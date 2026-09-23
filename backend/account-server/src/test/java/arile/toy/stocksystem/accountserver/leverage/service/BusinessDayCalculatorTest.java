package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.market.holiday.HolidayRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BusinessDayCalculatorTest {

    private static final LocalDate FRI = LocalDate.of(2026, 9, 18);
    private static final LocalDate MON = LocalDate.of(2026, 9, 21);
    private static final LocalDate TUE = LocalDate.of(2026, 9, 22);
    private static final LocalDate THU = LocalDate.of(2026, 9, 24);

    @Mock
    private HolidayRegistry holidayRegistry;

    @InjectMocks
    private BusinessDayCalculator calculator;

    @ParameterizedTest(name = "{0} → {1}: {2}영업일")
    @CsvSource({
            "2026-09-21, 2026-09-24, 3", // 월 -> 목: 화·수·목
            "2026-09-18, 2026-09-21, 1", // 금 -> 월: 토·일 제외, 월만
            "2026-09-21, 2026-09-21, 0", // 같은 날
            "2026-09-24, 2026-09-21, 0"  // 시작일이 오늘보다 뒤
    })
    @DisplayName("businessDaysElapsed: 시작일 다음 날부터 오늘까지 주말을 제외한 영업일 수를 센다")
    void businessDaysElapsed(LocalDate from, LocalDate today, int expected) {
        assertThat(calculator.businessDaysElapsed(from, today)).isEqualTo(expected);
    }

    @Test
    @DisplayName("businessDaysElapsed: 관리자가 등록한 평일 휴장일도 제외한다")
    void businessDaysElapsed_excludesHoliday() {
        given(holidayRegistry.isHoliday(TUE)).willReturn(true);
        given(holidayRegistry.isHoliday(TUE.plusDays(1))).willReturn(false); // 수
        given(holidayRegistry.isHoliday(THU)).willReturn(false);

        assertThat(calculator.businessDaysElapsed(MON, THU)).isEqualTo(2);
    }

    @Test
    @DisplayName("isBusinessDay: 주말은 휴장일 조회 없이 영업일이 아니다")
    void isBusinessDay_weekend() {
        assertThat(calculator.isBusinessDay(FRI.plusDays(1))).isFalse(); // 토
        assertThat(calculator.isBusinessDay(FRI.plusDays(2))).isFalse(); // 일

        verifyNoInteractions(holidayRegistry);
    }

    @Test
    @DisplayName("isBusinessDay: 평일은 휴장일 여부에 따라 판정한다")
    void isBusinessDay_weekday() {
        given(holidayRegistry.isHoliday(MON)).willReturn(false);
        given(holidayRegistry.isHoliday(TUE)).willReturn(true);

        assertThat(calculator.isBusinessDay(MON)).isTrue();
        assertThat(calculator.isBusinessDay(TUE)).isFalse();
    }
}
