package arile.toy.stocksystem.bffserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisMarketPhaseSubscribersTest {

    private static Message message(String channel, String body) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), body.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("종목별 장 상태")
    class StockPhase {

        private final BffServerMarketPhaseRegistry registry = mock(BffServerMarketPhaseRegistry.class);
        private final RedisMarketPhaseEventSubscriber subscriber =
                new RedisMarketPhaseEventSubscriber(new ObjectMapper(), registry);

        @Test
        @DisplayName("종목별 장 상태 메시지를 캐시에 반영한다")
        void onMessage() {
            subscriber.onMessage(message("market-phase", """
                    {"stockCode": "005930", "marketPhase": "CLOSING_CALL"}
                    """), null);

            verify(registry).setPhase("005930", BffServerMarketPhase.CLOSING_CALL);
        }

        @Test
        @DisplayName("JSON이 깨져 있거나 알 수 없는 장 상태면 캐시를 바꾸지 않는다")
        void invalid_ignored() {
            subscriber.onMessage(message("market-phase", "{broken"), null);
            subscriber.onMessage(message("market-phase", """
                    {"stockCode": "005930", "marketPhase": "LUNCH"}
                    """), null);

            verifyNoInteractions(registry);
        }
    }

    @Nested
    @DisplayName("전체 장 상태")
    class GlobalPhase {

        private final GlobalMarketPhasePushService pushService = mock(GlobalMarketPhasePushService.class);
        private final BffServerMarketPhaseRegistry registry = mock(BffServerMarketPhaseRegistry.class);
        private final RedisGlobalMarketPhaseEventSubscriber subscriber =
                new RedisGlobalMarketPhaseEventSubscriber(pushService, registry);

        @Test
        @DisplayName("전체 장 상태를 캐시에 반영하고 접속자 전원에게 푸시한다")
        void onMessage() {
            subscriber.onMessage(message("market:global-phase", "OPEN"), null);

            verify(registry).setGlobalPhase(BffServerMarketPhase.OPEN);
            verify(pushService).push("OPEN");
        }

        @Test
        @DisplayName("[현재 동작] 알 수 없는 장 상태면 캐시는 바꾸지 않지만, 푸시는 그대로 시도한다")
        void unknownPhase_stillPushes() {
            subscriber.onMessage(message("market:global-phase", "LUNCH"), null);

            verify(registry, never()).setGlobalPhase(any());
            verify(pushService).push("LUNCH");
        }

        @Test
        @DisplayName("푸시가 실패해도 예외를 던지지 않는다")
        void pushFails_swallowed() {
            willThrow(new IllegalStateException("boom")).given(pushService).push("OPEN");

            assertThatCode(() -> subscriber.onMessage(message("market:global-phase", "OPEN"), null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("장 마감")
    class MarketClose {

        private final MarketCloseEventPushService pushService = mock(MarketCloseEventPushService.class);
        private final RedisMarketCloseEventSubscriber subscriber = new RedisMarketCloseEventSubscriber(pushService);

        @Test
        @DisplayName("장 마감 메시지 본문(세션 구분)을 그대로 푸시 서비스에 넘긴다")
        void onMessage() {
            subscriber.onMessage(message("market:close", "REGULAR"), null);

            verify(pushService).push("REGULAR");
        }

        @Test
        @DisplayName("푸시가 실패해도 예외를 던지지 않는다")
        void pushFails_swallowed() {
            willThrow(new IllegalStateException("boom")).given(pushService).push("REGULAR");

            assertThatCode(() -> subscriber.onMessage(message("market:close", "REGULAR"), null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("이자 청구 완료 · 랭크 갱신 완료")
    class BatchCompleted {

        @Test
        @DisplayName("이자 청구 완료 신호를 받으면 본문과 관계없이 공지를 푸시한다")
        void interestApplied() {
            InterestAppliedPushService pushService = mock(InterestAppliedPushService.class);

            new RedisInterestAppliedEventSubscriber(pushService)
                    .onMessage(message("account:interest-applied", "{\"any\":\"body\"}"), null);

            verify(pushService).push();
        }

        @Test
        @DisplayName("랭크 갱신 완료 신호를 받으면 본문과 관계없이 공지를 푸시한다")
        void rankUpdated() {
            RankUpdatedPushService pushService = mock(RankUpdatedPushService.class);

            new RedisRankUpdatedEventSubscriber(pushService)
                    .onMessage(message("account:rank-updated", "{\"any\":\"body\"}"), null);

            verify(pushService).push();
        }

        @Test
        @DisplayName("공지 푸시가 실패해도 예외를 던지지 않는다")
        void pushFails_swallowed() {
            InterestAppliedPushService interest = mock(InterestAppliedPushService.class);
            RankUpdatedPushService rank = mock(RankUpdatedPushService.class);
            willThrow(new IllegalStateException("boom")).given(interest).push();
            willThrow(new IllegalStateException("boom")).given(rank).push();

            assertThatCode(() -> new RedisInterestAppliedEventSubscriber(interest)
                    .onMessage(message("account:interest-applied", "{}"), null)).doesNotThrowAnyException();
            assertThatCode(() -> new RedisRankUpdatedEventSubscriber(rank)
                    .onMessage(message("account:rank-updated", "{}"), null)).doesNotThrowAnyException();
        }
    }
}
