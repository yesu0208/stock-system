package arile.toy.stocksystem.bffserver.stocktalk.dto;

import java.util.List;

public record StockTalkJoinResponse(
        int participantCount,
        List<StockTalkMessage> messages
) {
    public static StockTalkJoinResponse of(int participantCount, List<StockTalkMessage> messages) {
        return new StockTalkJoinResponse(participantCount, messages);
    }
}
