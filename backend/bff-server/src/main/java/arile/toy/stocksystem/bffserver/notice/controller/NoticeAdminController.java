package arile.toy.stocksystem.bffserver.notice.controller;

import arile.toy.stocksystem.bffserver.notice.dto.NoticeCreateRequest;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeDetail;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeEditRequest;
import arile.toy.stocksystem.bffserver.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeDetail> createNotice(
            @AuthenticationPrincipal UserDetails admin,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        var created = noticeService.createNotice(admin.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{noticeId}")
    public ResponseEntity<NoticeDetail> editNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeEditRequest request
    ) {
        return ResponseEntity.ok(noticeService.editNotice(noticeId, request));
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
}
