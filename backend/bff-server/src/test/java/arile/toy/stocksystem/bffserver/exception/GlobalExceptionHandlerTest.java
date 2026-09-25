package arile.toy.stocksystem.bffserver.exception;

import arile.toy.stocksystem.bffserver.exception.dto.ErrorResponse;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private SlackNotifier slackNotifier;

    @InjectMocks
    private GlobalExceptionHandler handler;

    private static MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/api/v1/orders");
    }

    /** MethodArgumentNotValidException 생성용 더미 메서드 */
    @SuppressWarnings("unused")
    private void dummy(String body) {
    }

    @Test
    @DisplayName("ClientErrorException: 예외에 담긴 상태 코드와 메시지로 응답하고 Slack 알림은 보내지 않는다")
    void clientError() {
        ClientErrorException exception = new ClientErrorException(HttpStatus.CONFLICT, "이미 존재하는 유저입니다.");

        ResponseEntity<ErrorResponse> response = handler.handleClientErrorException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("이미 존재하는 유저입니다.");
        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException: 필드별 검증 메시지를 모아 400으로 응답한다")
    void methodArgumentNotValid() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "아이디는 소문자, 숫자만 가능"));
        bindingResult.addError(new FieldError("request", "password", "비밀번호는 최소 8자 이상이어야 합니다"));
        MethodParameter parameter =
                new MethodParameter(getClass().getDeclaredMethod("dummy", String.class), 0);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(
                new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .contains("username: 아이디는 소문자, 숫자만 가능")
                .contains("password: 비밀번호는 최소 8자 이상이어야 합니다");
        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("HttpMessageNotReadableException: 요청 본문이 없거나 깨졌으면 400으로 응답한다")
    void messageNotReadable() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("broken", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Required request body is missing.");
        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("처리되지 않은 RuntimeException: 500으로 응답하고 요청 경로와 함께 Slack으로 알린다")
    void runtimeException() {
        IllegalStateException exception = new IllegalStateException("boom");

        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
        verify(slackNotifier).notifyServerError("/api/v1/orders", exception);
    }

    @Test
    @DisplayName("처리되지 않은 checked Exception: 500으로 응답하고 Slack으로 알린다")
    void checkedException() {
        Exception exception = new Exception("checked");

        ResponseEntity<ErrorResponse> response = handler.handleException(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(slackNotifier).notifyServerError("/api/v1/orders", exception);
    }

    @Test
    @DisplayName("Slack 알림 전송이 실패해도 원래 에러 응답(500)은 그대로 반환한다")
    void slackFailure_stillResponds() {
        willThrow(new RuntimeException("slack down")).given(slackNotifier).notifyServerError(anyString(), any());

        ResponseEntity<ErrorResponse> response =
                handler.handleRuntimeException(new IllegalStateException("boom"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("컨트롤러 파라미터가 아닌 타입 오류는 바인딩 속성 이름을 담아 400으로 응답한다")
    void typeMismatch_propertyName() {
        TypeMismatchException exception = new TypeMismatchException("abc", Long.class);
        exception.initPropertyName("quantity");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Invalid value for parameter 'quantity'.");
    }

    @Test
    @DisplayName("HttpStatus에 정의되지 않은 상태 코드의 Spring 예외는 400으로 응답하고 Slack 알림을 보내지 않는다")
    void nonStandardStatus_badRequest() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatusCode.valueOf(499), "client closed");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");

        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(slackNotifier);
    }
}
