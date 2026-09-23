package arile.toy.stocksystem.bffserver.alertcancel.controller;

import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelRequest;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelResponse;
import arile.toy.stocksystem.bffserver.alertcancel.service.AlertCancelIngressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/alerts/cancel")
@RequiredArgsConstructor
public class AlertCancelController {

    private final AlertCancelIngressService alertCancelIngressService;

    @PostMapping
    public ResponseEntity<AlertCancelResponse> cancel(
            @Valid @RequestBody AlertCancelRequest alertCancelRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 요청자를 함께 전달해 stock-server에서 알림 소유자 확인 (타인 알림 취소 방지)
        AlertCancelResponse alertCancelResponse =
                alertCancelIngressService.receive(user.getUsername(), alertCancelRequest);

        return ResponseEntity.ok(alertCancelResponse);
    }
}
