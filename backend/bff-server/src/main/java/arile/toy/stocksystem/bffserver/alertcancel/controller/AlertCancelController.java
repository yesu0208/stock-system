package arile.toy.stocksystem.bffserver.alertcancel.controller;

import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelRequest;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelResponse;
import arile.toy.stocksystem.bffserver.alertcancel.service.AlertCancelIngressService;
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
            @RequestBody AlertCancelRequest alertCancelRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AlertCancelResponse alertCancelResponse =
                alertCancelIngressService.receive(alertCancelRequest);

        return ResponseEntity.ok(alertCancelResponse);
    }
}
