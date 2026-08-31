package arile.toy.stocksystem.bffserver.stocktalk.dto;

import java.util.List;

public record StockTalkJoinResponse(
        String ticker,
        int participantCount,
        List<StockTalkMessage> messages
) {
    public static StockTalkJoinResponse of(String ticker, int participantCount, List<StockTalkMessage> messages) {
        return new StockTalkJoinResponse(ticker, participantCount, messages);
    }
}
