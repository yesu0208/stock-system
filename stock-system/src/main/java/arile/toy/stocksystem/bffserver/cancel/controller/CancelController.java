package arile.toy.stocksystem.bffserver.cancel.controller;

import arile.toy.stocksystem.bffserver.cancel.dto.CancelRequest;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelResponse;
import arile.toy.stocksystem.bffserver.cancel.service.CancelIngressService;
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
@RequestMapping("api/v1/cancels")
@RequiredArgsConstructor
public class CancelController {

    private final CancelIngressService cancelIngressService;

    @PostMapping
    public ResponseEntity<CancelResponse> cancel(
            @RequestBody CancelRequest cancelRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CancelResponse cancelResponse =
                cancelIngressService.receive(cancelRequest);

        return ResponseEntity.ok(cancelResponse);
    }
}
