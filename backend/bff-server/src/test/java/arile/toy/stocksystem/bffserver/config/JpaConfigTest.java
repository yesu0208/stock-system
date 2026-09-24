package arile.toy.stocksystem.bffserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaConfigTest {

    @Test
    @DisplayName("Auditing 작성자는 항상 고정값 arile이다")
    void auditorAware() {
        assertThat(new JpaConfig().auditorAware().getCurrentAuditor()).contains("arile");
    }
}
