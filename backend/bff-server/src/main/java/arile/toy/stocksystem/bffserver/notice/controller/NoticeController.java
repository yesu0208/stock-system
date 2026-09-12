package arile.toy.stocksystem.bffserver.notice.controller;

import arile.toy.stocksystem.bffserver.notice.dto.CursorPage;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeDetail;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeSummary;
import arile.toy.stocksystem.bffserver.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<CursorPage<NoticeSummary>> getNotices(
            @RequestParam(required = false) Long cursor
    ) {
        return ResponseEntity.ok(noticeService.getNotices(cursor));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetail> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(noticeId));
    }
}
