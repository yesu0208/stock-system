package arile.toy.stocksystem.bffserver.stocktalk.controller;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkSendRequest;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkSessionRegistry;
import arile.toy.stocksystem.bffserver.stocktalk.service.StockTalkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StockTalkControllerTest {

    private static final Principal USER1 = () -> "user1";

    private StockTalkService stockTalkService;
    private StockTalkSessionRegistry sessionRegistry;
    private StockTalkController controller;

    @BeforeEach
    void setUp() {
        stockTalkService = mock(StockTalkService.class);
        sessionRegistry = new StockTalkSessionRegistry();
        controller = new StockTalkController(stockTalkService, sessionRegistry);
    }

    @Test
    @DisplayName("join: 세션의 첫 입장이면 방에 입장시킨다")
    void join() {
        controller.join("005930", USER1, "session-A");

        verify(stockTalkService).join("005930", "user1", "session-A");
    }

    @Test
    @DisplayName("join: 같은 세션이 다시 입장하면 방 카운트는 올리지 않고 최근 메시지만 다시 보낸다 (유령 참여자 방지)")
    void join_sameSessionTwice() {
        controller.join("005930", USER1, "session-A");
        controller.join("005930", USER1, "session-A");

        verify(stockTalkService, times(1)).join("005930", "user1", "session-A");
        verify(stockTalkService).sendHistory("005930", "user1", "session-A");
    }

    @Test
    @DisplayName("leave: 입장해 있던 세션이면 방에서 퇴장시킨다")
    void leave() {
        controller.join("005930", USER1, "session-A");

        controller.leave("005930", USER1, "session-A");

        verify(stockTalkService).leave("005930", "user1");
    }

    @Test
    @DisplayName("leave: 입장한 적 없는 세션의 퇴장 요청은 무시한다 (다른 탭의 참여를 깎지 않음)")
    void leave_withoutJoin_ignored() {
        controller.join("005930", USER1, "session-A");

        controller.leave("005930", USER1, "session-B");

        verify(stockTalkService, never()).leave("005930", "user1");
    }

    @Test
    @DisplayName("send: 로그인 사용자의 채팅을 서비스에 넘긴다")
    void send() {
        controller.send("005930", new StockTalkSendRequest("안녕하세요"), USER1);

        verify(stockTalkService).sendMessage("005930", "user1", "안녕하세요");
    }

    @Test
    @DisplayName("익명 연결의 입장·퇴장·채팅은 모두 무시한다 (종목톡은 로그인 사용자 전용)")
    void anonymous_ignored() {
        controller.join("005930", null, "session-A");
        controller.leave("005930", null, "session-A");
        controller.send("005930", new StockTalkSendRequest("익명"), null);

        verifyNoInteractions(stockTalkService);
    }
}
