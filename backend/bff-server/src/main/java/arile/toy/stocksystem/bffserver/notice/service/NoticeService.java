package arile.toy.stocksystem.bffserver.notice.service;

import arile.toy.stocksystem.bffserver.exception.notice.NoticeNotFoundException;
import arile.toy.stocksystem.bffserver.notice.dto.CursorPage;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeCreateRequest;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeDetail;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeEditRequest;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeSummary;
import arile.toy.stocksystem.bffserver.notice.entity.NoticeEntity;
import arile.toy.stocksystem.bffserver.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final int PAGE_SIZE = 20;

    private final NoticeRepository noticeRepository;

    @Transactional
    public NoticeDetail createNotice(String authorId, NoticeCreateRequest request) {
        var entity = NoticeEntity.of(request.title(), request.content(), authorId);
        var saved = noticeRepository.save(entity);
        return NoticeDetail.of(saved);
    }

    public NoticeDetail getNotice(Long noticeId) {
        return NoticeDetail.of(getNoticeEntity(noticeId));
    }

    @Transactional
    public NoticeDetail editNotice(Long noticeId, NoticeEditRequest request) {
        var notice = getNoticeEntity(noticeId);
        notice.edit(request.title(), request.content());
        return NoticeDetail.of(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        var notice = getNoticeEntity(noticeId);
        noticeRepository.delete(notice);
    }

    public CursorPage<NoticeSummary> getNotices(Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);
        List<NoticeEntity> notices = noticeRepository.findAllByCursor(cursor, pageable);

        boolean hasNext = notices.size() > PAGE_SIZE;
        List<NoticeEntity> page = hasNext ? notices.subList(0, PAGE_SIZE) : notices;

        if (page.isEmpty()) {
            return new CursorPage<>(List.of(), null, false);
        }

        List<NoticeSummary> items = page.stream()
                .map(NoticeSummary::of)
                .toList();

        Long nextCursor = hasNext ? page.get(page.size() - 1).getNoticeId() : null;
        return new CursorPage<>(items, nextCursor, hasNext);
    }

    private NoticeEntity getNoticeEntity(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));
    }
}
