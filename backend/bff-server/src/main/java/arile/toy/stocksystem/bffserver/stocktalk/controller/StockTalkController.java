package arile.toy.stocksystem.bffserver.stocktalk.controller;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkSendRequest;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkSessionRegistry;
import arile.toy.stocksystem.bffserver.stocktalk.service.StockTalkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 클라이언트 → 서버 STOMP 메시지 핸들러
 *
 * 클라이언트가 구독할 destination
 *  - 채팅 수신 : /sub/stock-talk/{ticker}
 *  - 히스토리  : /user/sub/stock-talk/history (본인 전용)
 *
 * 클라이언트가 전송할 destination
 *  - 입장 : /app/stock-talk/{ticker}/join
 *  - 퇴장 : /app/stock-talk/{ticker}/leave
 *  - 채팅 : /app/stock-talk/{ticker}/send (body: StockTalkSendRequest)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StockTalkController {

    private final StockTalkService stockTalkService;
    private final StockTalkSessionRegistry sessionRegistry;

    @MessageMapping("/stock-talk/{ticker}/join")
    public void join(
            @DestinationVariable String ticker,
            Principal principal,
            @Header("simpSessionId") String sessionId
    ) {
        String username = requireUsername(principal, ticker, "join");
        if (username == null) return;

        stockTalkService.join(ticker, username, sessionId);
        sessionRegistry.registerJoin(sessionId, username, ticker);
    }

    @MessageMapping("/stock-talk/{ticker}/leave")
    public void leave(
            @DestinationVariable String ticker,
            Principal principal,
            @Header("simpSessionId") String sessionId
    ) {
        String username = requireUsername(principal, ticker, "leave");
        if (username == null) return;

        stockTalkService.leave(ticker, username);
        sessionRegistry.registerLeave(sessionId, ticker);
    }

    @MessageMapping("/stock-talk/{ticker}/send")
    public void send(
            @DestinationVariable String ticker,
            @Valid @Payload StockTalkSendRequest request,
            Principal principal
    ) {
        String username = requireUsername(principal, ticker, "send");
        if (username == null) return;

        stockTalkService.sendMessage(ticker, username, request.content());
    }

    /**
     * 종목톡은 로그인 사용자 전용 기능이다.
     * StompJwtChannelInterceptor는 익명 연결(토큰 없는 CONNECT)도 허용하므로,
     * 각 메시지 핸들러에서 principal이 없는 경우를 직접 방어한다.
     */
    private String requireUsername(Principal principal, String ticker, String action) {
        if (principal == null) {
            log.warn("[StockTalk] anonymous user tried to {} on ticker={}", action, ticker);
            return null;
        }
        return principal.getName();
    }
}
