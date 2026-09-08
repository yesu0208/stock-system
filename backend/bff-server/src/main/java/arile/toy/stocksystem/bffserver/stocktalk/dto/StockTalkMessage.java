package arile.toy.stocksystem.bffserver.stocktalk.dto;

import java.time.Instant;

public record StockTalkMessage(
        StockTalkMessageType type,
        String ticker,
        String sender,
        String senderNickname,
        String senderProfileImageUrl,
        String content,
        Instant sentAt,
        int participantCount
) {
    public static StockTalkMessage chat(
            String ticker, String sender, String senderNickname, String senderProfileImageUrl,
            String content, int participantCount
    ) {
        return new StockTalkMessage(
                StockTalkMessageType.CHAT, ticker, sender, senderNickname, senderProfileImageUrl,
                content, Instant.now(), participantCount);
    }

    public static StockTalkMessage enter(
            String ticker, String sender, String senderNickname, String senderProfileImageUrl,
            int participantCount
    ) {
        return new StockTalkMessage(
                StockTalkMessageType.ENTER, ticker, sender, senderNickname, senderProfileImageUrl,
                senderNickname + "님이 입장하셨습니다.", Instant.now(), participantCount);
    }

    public static StockTalkMessage leave(
            String ticker, String sender, String senderNickname, String senderProfileImageUrl,
            int participantCount
    ) {
        return new StockTalkMessage(
                StockTalkMessageType.LEAVE, ticker, sender, senderNickname, senderProfileImageUrl,
                senderNickname + "님이 퇴장하셨습니다.", Instant.now(), participantCount);
    }
}
