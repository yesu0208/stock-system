package arile.toy.stocksystem.bffserver.common;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import arile.toy.stocksystem.bffserver.dailyreturn.dto.DailyReturnHistoryItem;
import arile.toy.stocksystem.bffserver.external.stock.event.PriceLevel;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 프론트로 나가는 응답 record를 실제로 만들어 JSON에 값이 담기는지 확인 */
class ResponseRecordsSerializationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("알림 목록 항목: 알림 ID·종목·방향·발동 가격이 JSON에 담긴다")
    void alertResponseMessage() throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(new AlertResponseMessage(
                1L, "user1", "005930", AlertDirection.ABOVE, 70_000, Instant.parse("2026-09-24T00:30:00Z")));

        assertThat(json).contains("\"005930\"", "\"ABOVE\"", "70000");
    }

    @Test
    @DisplayName("호가: 매도·매수 호가(가격·잔량)를 읽고, 같은 모양으로 다시 JSON에 담는다")
    void bidAskPriceTickMessage() throws Exception {
        String json = """
                {"tickMessageType": "%s", "stockCode": "005930",
                 "asks": [{"price": 71100, "quantity": 300}, {"price": 71200, "quantity": 500}],
                 "bids": [{"price": 71000, "quantity": 400}],
                 "totalAskNum": 800, "totalBidNum": 400}
                """.formatted(TickMessageType.values()[0].name());

        BffServerBidAskPriceTickMessage message = OBJECT_MAPPER.readValue(json, BffServerBidAskPriceTickMessage.class);

        assertThat(message.asks()).containsExactly(new PriceLevel(71_100, 300), new PriceLevel(71_200, 500));
        assertThat(message.bids()).containsExactly(new PriceLevel(71_000, 400));
        assertThat(message.totalAskNum()).isEqualTo(800);
        assertThat(OBJECT_MAPPER.writeValueAsString(message))
                .contains("\"price\":71100", "\"quantity\":300", "\"totalBidNum\":400");
    }

    @Test
    @DisplayName("일일 수익률 항목: 날짜·금액·수익률이 JSON에 담긴다")
    void dailyReturnHistoryItem() throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(new DailyReturnHistoryItem(
                LocalDate.of(2026, 9, 24), 1_000_000L, 1_010_000L, 10_000L, 1.0, 5_000L, 5_000L, 0.5));

        assertThat(json).contains("1010000", "10000");
    }
}
