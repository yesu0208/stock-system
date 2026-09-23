package arile.toy.stocksystem.bffserver.user.contoller;

import arile.toy.stocksystem.bffserver.admin.service.AdminAccessService;
import arile.toy.stocksystem.bffserver.exception.user.UserAlreadyExistsException;
import arile.toy.stocksystem.bffserver.rank.client.RankApiClient;
import arile.toy.stocksystem.bffserver.rank.dto.RankHistoryResponse;
import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.ChangeNicknameRequest;
import arile.toy.stocksystem.bffserver.user.dto.ChangePasswordRequest;
import arile.toy.stocksystem.bffserver.user.dto.Role;
import arile.toy.stocksystem.bffserver.user.dto.UserAuthenticationResponse;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class UserControllerTest {

    private static final String BASE = "/api/v1/users";
    private static final String SIGN_UP_BODY = """
            {"username": "user1", "nickname": "닉네임", "password": "password1!"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AdminAccessService adminAccessService;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;
    @MockitoBean private RankApiClient rankApiClient;
    @MockitoBean private SlackNotifier slackNotifier;

    private static UserDto dto(String nickname) {
        return new UserDto(1L, "user1", nickname, Instant.parse("2026-09-01T00:00:00Z"), Role.USER, null, null);
    }

    // ===================== 회원가입 · 로그인 · 로그아웃 =====================

    @Nested
    @DisplayName("회원가입 · 로그인 · 로그아웃")
    class Auth {

        @Test
        @DisplayName("POST /users: 유효한 요청이면 인증 없이 가입하고, 비밀번호 없이 사용자 정보를 반환한다")
        void signUp() throws Exception {
            given(userService.signUp(any())).willReturn(dto("닉네임"));

            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("user1"))
                    .andExpect(jsonPath("$.nickname").value("닉네임"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "아이디 대문자         | {\"username\": \"User1\", \"nickname\": \"닉네임\", \"password\": \"password1!\"}",
                "아이디 3자            | {\"username\": \"abc\", \"nickname\": \"닉네임\", \"password\": \"password1!\"}",
                "닉네임 특수문자       | {\"username\": \"user1\", \"nickname\": \"닉네임!\", \"password\": \"password1!\"}",
                "비밀번호 7자          | {\"username\": \"user1\", \"nickname\": \"닉네임\", \"password\": \"pass1!a\"}",
                "비밀번호 특수문자 없음 | {\"username\": \"user1\", \"nickname\": \"닉네임\", \"password\": \"password1\"}"
        })
        @DisplayName("POST /users: 입력 규칙을 어기면 400을 반환하고 가입하지 않는다")
        void signUp_invalid(String description, String body) throws Exception {
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("POST /users: 이미 있는 아이디면 예외에 정의된 상태 코드로 응답한다")
        void signUp_duplicate() throws Exception {
            UserAlreadyExistsException exception = new UserAlreadyExistsException();
            given(userService.signUp(any())).willThrow(exception);

            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY))
                    .andExpect(status().is(exception.getStatus().value()));
        }

        @Test
        @DisplayName("POST /users/authenticate: 로그인하면 액세스 토큰을 반환한다")
        void authenticate() throws Exception {
            given(userService.authenticate(any(), any())).willReturn(new UserAuthenticationResponse("access"));

            mockMvc.perform(post(BASE + "/authenticate").contentType(MediaType.APPLICATION_JSON).content("""
                            {"username": "user1", "password": "password1!"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access"));
        }

        @Test
        @DisplayName("POST /users/logout: 리프레시 토큰을 폐기하고 쿠키를 즉시 만료시킨다")
        void logout() throws Exception {
            given(jwtService.getJtiFromRefreshToken("refresh")).willReturn("jti-1");

            mockMvc.perform(post(BASE + "/logout")
                            .cookie(new Cookie("refreshToken", "refresh"))
                            .with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("refreshToken", 0))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().path("refreshToken", "/"));

            verify(refreshTokenRepository).delete("jti-1");
        }

        @Test
        @DisplayName("POST /users/logout: 리프레시 토큰 해석에 실패해도 쿠키는 만료시킨다")
        void logout_invalidToken() throws Exception {
            willThrow(new RuntimeException("invalid")).given(jwtService).getJtiFromRefreshToken("broken");

            mockMvc.perform(post(BASE + "/logout")
                            .cookie(new Cookie("refreshToken", "broken"))
                            .with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("refreshToken", 0));

            verify(refreshTokenRepository, never()).delete(anyString());
        }

        @Test
        @DisplayName("POST /users/logout: 쿠키가 없어도 만료 쿠키를 내려준다")
        void logout_withoutCookie() throws Exception {
            mockMvc.perform(post(BASE + "/logout").with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("refreshToken", 0));

            verifyNoInteractions(jwtService, refreshTokenRepository);
        }
    }

    // ===================== 조회 =====================

    @Nested
    @DisplayName("조회")
    class Query {

        @Test
        @DisplayName("GET /users/check-username: 인증 없이 아이디 중복 여부를 반환한다")
        void checkUsername() throws Exception {
            given(userService.isUsernameExists("user1")).willReturn(true);

            mockMvc.perform(get(BASE + "/check-username").param("username", "user1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists").value(true));
        }

        @Test
        @DisplayName("GET /users/check-nickname: 인증 없이 닉네임 중복 여부를 반환한다")
        void checkNickname() throws Exception {
            given(userService.isNicknameExists("닉네임")).willReturn(false);

            mockMvc.perform(get(BASE + "/check-nickname").param("nickname", "닉네임"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists").value(false));
        }

        @Test
        @DisplayName("GET /users/all: 관리자는 전체 사용자 목록을 조회한다")
        void getAllUsers() throws Exception {
            given(userService.getAllUsers()).willReturn(List.of(dto("a"), dto("b")));

            mockMvc.perform(get(BASE + "/all").with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("GET /users/user: 내 정보에 랭크를 붙여 반환한다")
        void getUser() throws Exception {
            given(userService.getUserByUsername("user1")).willReturn(dto("닉네임"));
            given(rankApiClient.getRank("user1"))
                    .willReturn(new RankResponse("user1", "GOLD", 4, 3800L, "PLATINUM_5", 3750L, 4250L));

            mockMvc.perform(get(BASE + "/user").with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("닉네임"))
                    .andExpect(jsonPath("$.rank.tier").value("GOLD"));
        }

        @Test
        @DisplayName("GET /users/user: 랭크 서버 호출이 실패해 null이어도 내 정보는 반환한다")
        void getUser_rankUnavailable() throws Exception {
            given(userService.getUserByUsername("user1")).willReturn(dto("닉네임"));
            given(rankApiClient.getRank("user1")).willReturn(null);

            mockMvc.perform(get(BASE + "/user").with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("닉네임"))
                    .andExpect(jsonPath("$.rank").doesNotExist());
        }

        @ParameterizedTest(name = "page={0} → {1}")
        @CsvSource({"1, 0", "3, 2", "0, 0"})
        @DisplayName("GET /users/rank/history: 1부터 시작하는 페이지를 0부터로 바꿔 조회한다 (0 이하는 0)")
        void getRankHistory(int page, int zeroBased) throws Exception {
            given(adminAccessService.resolveTargetUsername(any(), any())).willReturn("user1");
            given(rankApiClient.getRankHistory("user1", zeroBased, 20))
                    .willReturn(new RankHistoryResponse(List.of(), false));

            mockMvc.perform(get(BASE + "/rank/history").param("page", String.valueOf(page)).with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("GET /users/rank/history: username 파라미터로 대상 사용자를 해석해 조회한다")
        void getRankHistory_target() throws Exception {
            given(adminAccessService.resolveTargetUsername(any(), eq("target"))).willReturn("target");
            given(rankApiClient.getRankHistory("target", 0, 20)).willReturn(new RankHistoryResponse(List.of(), true));

            mockMvc.perform(get(BASE + "/rank/history").param("username", "target")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(true));
        }

        @Test
        @DisplayName("GET /users/rank/history: 랭크 서버 호출에 실패하면 500을 반환한다")
        void getRankHistory_apiFails() throws Exception {
            given(adminAccessService.resolveTargetUsername(any(), any())).willReturn("user1");
            given(rankApiClient.getRankHistory("user1", 0, 20)).willReturn(null);

            mockMvc.perform(get(BASE + "/rank/history").with(user("user1")))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ===================== 정보 변경 =====================

    @Nested
    @DisplayName("정보 변경")
    class Change {

        @Test
        @DisplayName("PATCH /users/password: 로그인한 사용자의 비밀번호를 바꾼다")
        void changePassword() throws Exception {
            given(userService.changePassword(eq("user1"), any())).willReturn(dto("닉네임"));

            mockMvc.perform(patch(BASE + "/password").with(user("user1"))
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"currentPassword": "password1!", "newPassword": "newpass1!!"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("user1"));
        }

        @Test
        @DisplayName("PATCH /users/nickname: 로그인한 사용자의 닉네임을 바꾼다")
        void changeNickname() throws Exception {
            given(userService.changeNickname(eq("user1"), any())).willReturn(dto("새닉네임"));

            mockMvc.perform(patch(BASE + "/nickname").with(user("user1"))
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"nickname": "새닉네임"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("새닉네임"));
        }

        @Test
        @DisplayName("POST /users/profile-image: 업로드한 이미지로 프로필을 바꾼다")
        void changeProfileImage() throws Exception {
            MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});
            UserDto updated = new UserDto(1L, "user1", "닉네임", Instant.parse("2026-09-01T00:00:00Z"),
                    Role.USER, null, "/uploads/profile/user1_x.png");
            given(userService.changeProfileImage(eq("user1"), any())).willReturn(updated);

            mockMvc.perform(multipart(BASE + "/profile-image").file(image).with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profileImageUrl").value("/uploads/profile/user1_x.png"));
        }
    }

    // ===================== 인증 주체 없음 (방어 코드) =====================

    @Nested
    @DisplayName("인증 주체가 없을 때 (SecurityConfig에 막혀 실제로는 도달하지 않는 방어 코드)")
    class NullPrincipal {

        private UserController controller() {
            return new UserController(userService, jwtService, adminAccessService, refreshTokenRepository, rankApiClient);
        }

        @Test
        @DisplayName("내 정보·랭크 이력·정보 변경 API는 인증 주체가 없으면 401을 반환한다")
        void returnsUnauthorized() {
            UserController controller = controller();
            MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1});

            assertThat(controller.getUser(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(controller.getRankHistory(null, null, 1, 20).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(controller.changePassword(new ChangePasswordRequest("password1!", "newpass1!!"), null)
                    .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(controller.changeNickname(new ChangeNicknameRequest("새닉네임"), null)
                    .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(controller.changeProfileImage(image, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(userService, rankApiClient, adminAccessService);
        }
    }
}
