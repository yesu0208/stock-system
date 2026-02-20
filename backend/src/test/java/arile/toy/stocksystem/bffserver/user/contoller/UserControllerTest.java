package arile.toy.stocksystem.bffserver.user.contoller;

import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.UserAuthenticationResponse;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.dto.UserLoginRequest;
import arile.toy.stocksystem.bffserver.user.dto.UserSignUpRequest;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("회원가입 성공 - 올바른 아이디와 비밀번호")
    void givenValidSignUpRequest_whenSignUp_thenReturnsUserDto() throws Exception {
        var request = new UserSignUpRequest("user1", "pass123!");
        var userDto = new UserDto(1L, "user1", Instant.now());

        given(userService.signUp(request)).willReturn(userDto);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Test
    @DisplayName("회원가입 실패 - password 조건 불만족")
    void givenInvalidPassword_whenSignUp_thenReturnsBadRequest() throws Exception {
        var request = new UserSignUpRequest("user1", "short");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - username 비어있음")
    void givenEmptyUsername_whenSignUp_thenReturnsBadRequest() throws Exception {
        var request = new UserSignUpRequest("", "pass123!");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공")
    void givenValidLoginRequest_whenAuthenticate_thenReturnsAuthResponse() throws Exception {
        var request = new UserLoginRequest("user1", "pass123");
        var authResponse = new UserAuthenticationResponse("accessToken123");

        given(userService.authenticate(eq(request), any(HttpServletResponse.class)))
                .willReturn(authResponse);

        mockMvc.perform(post("/api/v1/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken123"));
    }

    @Test
    @DisplayName("로그아웃 시 refreshToken 삭제 및 쿠키 초기화")
    void givenRefreshToken_whenLogout_thenDeletesTokenAndClearsCookie() throws Exception {
        String refreshToken = "oldToken";
        String jti = "jti123";

        given(jwtService.getJtiFromRefreshToken(refreshToken)).willReturn(jti);
        doNothing().when(refreshTokenRepository).delete(jti);

        mockMvc.perform(post("/api/v1/users/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refreshToken", nullValue()))
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(refreshTokenRepository).delete(jti);
    }

    @Test
    @DisplayName("사용자 이름 존재 여부를 확인한다")
    void givenUsername_whenCheckUsername_thenReturnsExists() throws Exception {
        String username = "user1";
        given(userService.isUsernameExists(username)).willReturn(true);

        mockMvc.perform(get("/api/v1/users/check-username")
                        .param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @DisplayName("모든 사용자를 조회한다")
    void whenGetAllUsers_thenReturnsUserList() throws Exception {
        List<UserDto> users = List.of(new UserDto(1L, "user1", Instant.now()),
                new UserDto(2L, "user2", Instant.now()));
        given(userService.getAllUsers()).willReturn(users);

        mockMvc.perform(get("/api/v1/users/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));
    }

    @Test
    @DisplayName("인증된 사용자 정보를 조회한다")
    @WithMockUser(username = "user1", roles = "USER")
    void givenAuthenticatedUser_whenGetUser_thenReturnsUserDto() throws Exception {
        UserDto userDto = new UserDto(1L, "user1", Instant.now());
        given(userService.getUserByUsername("user1")).willReturn(userDto);

        mockMvc.perform(get("/api/v1/users/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자 조회 시 401 반환한다")
    void givenUnauthenticatedUser_whenGetUser_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/user"))
                .andExpect(status().isUnauthorized());
    }
}
