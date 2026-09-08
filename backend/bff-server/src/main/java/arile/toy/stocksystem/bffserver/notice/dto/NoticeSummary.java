package arile.toy.stocksystem.bffserver.notice.dto;

import arile.toy.stocksystem.bffserver.notice.entity.NoticeEntity;

import java.time.Instant;

public record NoticeSummary(
        Long noticeId,
        String title,
        String authorId,
        Instant createdDateTime,
        String contentPreview
) {
    private static final int PREVIEW_LENGTH = 100;

    public static NoticeSummary of(NoticeEntity entity) {
        return new NoticeSummary(
                entity.getNoticeId(),
                entity.getTitle(),
                entity.getAuthorId(),
                entity.getCreatedDateTime(),
                preview(entity.getContent())
        );
    }

    private static String preview(String content) {
        return content.length() <= PREVIEW_LENGTH
                ? content
                : content.substring(0, PREVIEW_LENGTH) + "…";
    }
}
