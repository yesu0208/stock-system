package arile.toy.stocksystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private JwtExceptionFilter jwtExceptionFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests ->
                        requests
                                .requestMatchers(HttpMethod.POST, "/api/*/users", "/api/*/users/authenticate")
                                .permitAll()
                                .requestMatchers("/index.html", "/ws-stock/**")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/*/users/all")
                                .hasRole("ADMIN")
                                .anyRequest()
                                .authenticated())
                .sessionManagement(
                        (session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(CsrfConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, jwtAuthenticationFilter.getClass())
                .httpBasic(HttpBasicConfigurer::disable)
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .build();
    }
}
