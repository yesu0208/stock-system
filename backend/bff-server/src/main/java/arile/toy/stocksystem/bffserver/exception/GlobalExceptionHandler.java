package arile.toy.stocksystem.bffserver.exception;

import arile.toy.stocksystem.bffserver.exception.dto.ErrorResponse;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final SlackNotifier slackNotifier;

    @ExceptionHandler(ClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleClientErrorException(ClientErrorException exception) {
        return new ResponseEntity<>(
                new ErrorResponse(exception.getStatus(), exception.getMessage()), exception.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        var errorMessage =
                exception.getFieldErrors().stream()
                        .map(fieldError -> (fieldError.getField() + ": " + fieldError.getDefaultMessage()))
                        .toList()
                        .toString();

        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST, errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST, "Required request body is missing."), HttpStatus.BAD_REQUEST);
    }

    /**
     * 경로 변수·쿼리 파라미터의 타입 오류 (예: /discussions/abc).
     * Spring의 ErrorResponse를 구현하지 않아 아래 공통 처리로 걸러지지 않으므로 따로 400으로 응답.
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(TypeMismatchException exception) {
        String name = exception instanceof MethodArgumentTypeMismatchException argument
                ? argument.getName()
                : exception.getPropertyName();

        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + name + "'."),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException exception, HttpServletRequest request) {
        if (exception instanceof org.springframework.web.ErrorResponse springError) {
            return toSpringErrorResponse(springError);
        }

        log.error("Unhandled RuntimeException. path={}", request.getRequestURI(), exception);
        notifySafely(request.getRequestURI(), exception);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        if (exception instanceof org.springframework.web.ErrorResponse springError) {
            return toSpringErrorResponse(springError);
        }

        log.error("Unhandled Exception. path={}", request.getRequestURI(), exception);
        notifySafely(request.getRequestURI(), exception);
        return ResponseEntity.internalServerError().build();
    }

    /**
     * Spring MVC가 상태 코드를 정해 둔 요청 오류(없는 URL 404, 파라미터 누락 400, 메서드 불일치 405,
     * 업로드 용량 초과 413 등)는 서버 오류가 아니므로 그 상태 코드로 응답하고 Slack 알림은 보내지 않음.
     * (405의 Allow 헤더처럼 Spring이 붙이는 응답 헤더도 유지)
     */
    private ResponseEntity<ErrorResponse> toSpringErrorResponse(org.springframework.web.ErrorResponse springError) {
        HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        return new ResponseEntity<>(
                new ErrorResponse(status, springError.getBody().getDetail()),
                springError.getHeaders(),
                status);
    }

    private void notifySafely(String path, Exception exception) {
        try {
            slackNotifier.notifyServerError(path, exception);
        } catch (Exception e) {
            log.warn("Slack 알림 전송 실패", e);
        }
    }
}
