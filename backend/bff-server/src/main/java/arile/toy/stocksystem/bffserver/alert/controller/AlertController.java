package arile.toy.stocksystem.bffserver.alert.controller;

import arile.toy.stocksystem.bffserver.alert.dto.AlertRequest;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponse;
import arile.toy.stocksystem.bffserver.alert.service.AlertIngressService;
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
@RequestMapping("api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertIngressService alertIngressService;

    @PostMapping
    public ResponseEntity<AlertResponse> register(
            @Valid @RequestBody AlertRequest alertRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AlertResponse alertResponse =
                alertIngressService.receive(user.getUsername(), alertRequest);

        return ResponseEntity.ok(alertResponse);
    }
}
