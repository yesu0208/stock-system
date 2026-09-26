package arile.toy.stocksystem.stockserver.external.stock.listener;

import arile.toy.stocksystem.stockserver.external.stock.dispatcher.ExternalStockTickMessageDispatcher;
import jakarta.websocket.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Client] 외부 시세 웹소켓 클라이언트 테스트")
@ExtendWith(MockitoExtension.class)
class ExternalStockWebSocketClientTest {

    private static final String WS_URL = "ws://kis:21000";

    @Mock private ExternalStockTickMessageDispatcher dispatcher;
    @Mock private Session session;
    @Mock private RemoteEndpoint.Async asyncRemote;

    private ExternalStockWebSocketClient sut;

    @BeforeEach
    void setUp() {
        sut = new ExternalStockWebSocketClient(dispatcher);
        ReflectionTestUtils.setField(sut, "WS_URL", WS_URL);
    }

    @Nested
    @DisplayName("연결")
    class Connect {

        @DisplayName("웹소켓 컨테이너로 설정된 URL에 연결하고 세션을 보관한다")
        @Test
        void whenConnecting_thenStoresSession() throws Exception {
            WebSocketContainer container = mock(WebSocketContainer.class);
            given(container.connectToServer(sut, URI.create(WS_URL))).willReturn(session);
            given(session.isOpen()).willReturn(true);

            try (MockedStatic<ContainerProvider> provider = mockStatic(ContainerProvider.class)) {
                provider.when(ContainerProvider::getWebSocketContainer).thenReturn(container);

                sut.connect("approval-key");
            }

            assertThat(sut.isConnected()).isTrue();
        }

        @DisplayName("연결에 실패하면 IllegalStateException으로 감싸 던진다")
        @Test
        void givenConnectFails_whenConnecting_thenThrows() throws Exception {
            WebSocketContainer container = mock(WebSocketContainer.class);
            given(container.connectToServer(any(Object.class), any(URI.class))).willThrow(new IOException("refused"));

            try (MockedStatic<ContainerProvider> provider = mockStatic(ContainerProvider.class)) {
                provider.when(ContainerProvider::getWebSocketContainer).thenReturn(container);

                assertThatThrownBy(() -> sut.connect("approval-key"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasCauseInstanceOf(IOException.class);
            }
            assertThat(sut.isConnected()).isFalse();
        }

        @DisplayName("onOpen으로 받은 세션이 열려 있으면 연결 상태로 본다")
        @Test
        void whenOpened_thenConnected() {
            given(session.isOpen()).willReturn(true, false);

            sut.onOpen(session);

            assertThat(sut.isConnected()).isTrue();
            assertThat(sut.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("구독·수신")
    class SubscribeAndReceive {

        @DisplayName("세션이 열려 있으면 체결가(H0STCNT0)·호가(H0STASP0) 구독 메시지를 승인키와 함께 보낸다")
        @Test
        void givenOpenSession_whenSubscribing_thenSendsBothMessages() throws Exception {
            givenConnected("approval-key");
            given(session.getAsyncRemote()).willReturn(asyncRemote);

            sut.subscribe("005930");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            then(asyncRemote).should(times(2)).sendText(captor.capture(), any(SendHandler.class));
            List<String> messages = captor.getAllValues();
            assertThat(messages.get(0)).contains("\"tr_id\": \"H0STCNT0\"", "\"tr_key\": \"005930\"", "\"approval_key\": \"approval-key\"");
            assertThat(messages.get(1)).contains("\"tr_id\": \"H0STASP0\"", "\"tr_key\": \"005930\"");
        }

        @DisplayName("전송 결과가 실패여도 예외 없이 로그만 남긴다")
        @Test
        void givenSendFails_whenSubscribing_thenHandlesResult() throws Exception {
            givenConnected("approval-key");
            given(session.getAsyncRemote()).willReturn(asyncRemote);

            sut.subscribe("005930");

            ArgumentCaptor<SendHandler> handler = ArgumentCaptor.forClass(SendHandler.class);
            then(asyncRemote).should(times(2)).sendText(anyString(), handler.capture());
            assertThatNoException().isThrownBy(() -> {
                handler.getValue().onResult(new SendResult(new IOException("broken pipe")));
                handler.getValue().onResult(new SendResult());
            });
        }

        @DisplayName("세션이 없거나 닫혀 있으면 구독하지 않는다")
        @Test
        void givenNoOrClosedSession_whenSubscribing_thenSkips() {
            sut.subscribe("005930");

            sut.onOpen(session);
            given(session.isOpen()).willReturn(false);
            sut.subscribe("005930");

            then(session).should(never()).getAsyncRemote();
        }

        @DisplayName("수신 메시지를 분배기로 넘긴다")
        @Test
        void whenMessageReceived_thenDispatches() {
            sut.onMessage("0|H0STCNT0|1|x");

            then(dispatcher).should().dispatch("0|H0STCNT0|1|x");
        }
    }

    @Nested
    @DisplayName("연결 해제")
    class Disconnect {

        @DisplayName("세션이 없으면 아무것도 하지 않는다")
        @Test
        void givenNoSession_whenDisconnecting_thenNothing() {
            assertThatNoException().isThrownBy(() -> sut.disconnect());
        }

        @DisplayName("열린 세션은 정상 종료 코드로 닫고 세션을 비운다")
        @Test
        void givenOpenSession_whenDisconnecting_thenCloses() throws Exception {
            sut.onOpen(session);
            given(session.isOpen()).willReturn(true);

            sut.disconnect();

            ArgumentCaptor<CloseReason> reason = ArgumentCaptor.forClass(CloseReason.class);
            then(session).should().close(reason.capture());
            assertThat(reason.getValue().getCloseCode()).isEqualTo(CloseReason.CloseCodes.NORMAL_CLOSURE);
            assertThat(sut.isConnected()).isFalse();
        }

        @DisplayName("이미 닫힌 세션은 닫지 않고 세션만 비운다")
        @Test
        void givenClosedSession_whenDisconnecting_thenClearsOnly() throws Exception {
            sut.onOpen(session);
            given(session.isOpen()).willReturn(false);

            sut.disconnect();

            then(session).should(never()).close(any(CloseReason.class));
            assertThat(ReflectionTestUtils.getField(sut, "session")).isNull();
        }

        @DisplayName("닫기에 실패해도 세션은 비우고 IllegalStateException을 던진다")
        @Test
        void givenCloseFails_whenDisconnecting_thenThrowsAndClears() throws Exception {
            sut.onOpen(session);
            given(session.isOpen()).willReturn(true);
            willThrow(new IOException("close failed")).given(session).close(any(CloseReason.class));

            assertThatThrownBy(() -> sut.disconnect())
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(IOException.class);
            assertThat(ReflectionTestUtils.getField(sut, "session")).isNull();
        }
    }

    private void givenConnected(String approvalKey) throws Exception {
        WebSocketContainer container = mock(WebSocketContainer.class);
        given(container.connectToServer(sut, URI.create(WS_URL))).willReturn(session);
        given(session.isOpen()).willReturn(true);
        try (MockedStatic<ContainerProvider> provider = mockStatic(ContainerProvider.class)) {
            provider.when(ContainerProvider::getWebSocketContainer).thenReturn(container);
            sut.connect(approvalKey);
        }
    }
}
