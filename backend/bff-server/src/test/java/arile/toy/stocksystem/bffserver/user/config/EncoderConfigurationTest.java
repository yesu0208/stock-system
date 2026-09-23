package arile.toy.stocksystem.bffserver.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class EncoderConfigurationTest {

    private final BCryptPasswordEncoder encoder = new EncoderConfiguration().bCryptPasswordEncoder();

    @Test
    @DisplayName("BCrypt로 인코딩하며, 같은 비밀번호도 매번 다른 해시가 나온다 (salt)")
    void encode() {
        String first = encoder.encode("password1!");
        String second = encoder.encode("password1!");

        assertThat(first).startsWith("$2a$").isNotEqualTo(second);
    }

    @Test
    @DisplayName("원문과 해시를 비교해 일치 여부를 판정한다")
    void matches() {
        String hash = encoder.encode("password1!");

        assertThat(encoder.matches("password1!", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }
}
