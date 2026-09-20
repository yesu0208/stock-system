package arile.toy.stocksystem.bffserver.exception;

import arile.toy.stocksystem.bffserver.exception.dto.ErrorResponse;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException exception, HttpServletRequest request) {
        log.error("Unhandled RuntimeException. path={}", request.getRequestURI(), exception);
        notifySafely(request.getRequestURI(), exception);
        return ResponseEntity.internalServerError().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled Exception. path={}", request.getRequestURI(), exception);
        notifySafely(request.getRequestURI(), exception);
        return ResponseEntity.internalServerError().build();
    }

    private void notifySafely(String path, Exception exception) {
        try {
            slackNotifier.notifyServerError(path, exception);
        } catch (Exception e) {
            log.warn("Slack 알림 전송 실패", e);
        }
    }
}
