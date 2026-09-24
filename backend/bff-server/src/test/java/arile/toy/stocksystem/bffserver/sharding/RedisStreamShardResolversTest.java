package arile.toy.stocksystem.bffserver.sharding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RedisStreamShardResolversTest {

    /** (이름, 결정기 생성, 종목코드로 스트림 키 계산) */
    private record ResolverCase(String name,
                                Function<StockGroupRegistry, Object> factory,
                                java.util.function.BiFunction<Object, String, String> resolve) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Arguments> resolvers() {
        return Stream.of(
                Arguments.of(new ResolverCase("주문", RedisOrderStreamShardResolver::new,
                        (r, code) -> ((RedisOrderStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("주문 취소", RedisCancelStreamShardResolver::new,
                        (r, code) -> ((RedisCancelStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("자동 주문", RedisAutoOrderStreamShardResolver::new,
                        (r, code) -> ((RedisAutoOrderStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("자동 주문 취소", RedisAutoCancelStreamShardResolver::new,
                        (r, code) -> ((RedisAutoCancelStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("트레일링 스탑", RedisTrailingStopStreamShardResolver::new,
                        (r, code) -> ((RedisTrailingStopStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("트레일링 스탑 취소", RedisTrailingStopCancelStreamShardResolver::new,
                        (r, code) -> ((RedisTrailingStopCancelStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("OTOCO", RedisOtocoStreamShardResolver::new,
                        (r, code) -> ((RedisOtocoStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("OTOCO 취소", RedisOtocoCancelStreamShardResolver::new,
                        (r, code) -> ((RedisOtocoCancelStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("알림", RedisAlertStreamShardResolver::new,
                        (r, code) -> ((RedisAlertStreamShardResolver) r).resolveStreamKey(code))),
                Arguments.of(new ResolverCase("알림 해제", RedisAlertCancelStreamShardResolver::new,
                        (r, code) -> ((RedisAlertCancelStreamShardResolver) r).resolveStreamKey(code)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resolvers")
    @DisplayName("종목의 샤드 그룹을 찾아 '{prefix}-{그룹}' 형식의 스트림 키를 만든다")
    void resolveStreamKey(ResolverCase resolverCase) {
        StockGroupRegistry registry = mock(StockGroupRegistry.class);
        given(registry.resolveGroup("005930")).willReturn("A");

        Object resolver = resolverCase.factory().apply(registry);
        ReflectionTestUtils.setField(resolver, "prefix", "test-prefix");

        assertThat(resolverCase.resolve().apply(resolver, "005930")).isEqualTo("test-prefix-A");
    }
}
