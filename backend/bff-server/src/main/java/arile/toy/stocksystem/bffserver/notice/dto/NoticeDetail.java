package arile.toy.stocksystem.bffserver.notice.dto;

import arile.toy.stocksystem.bffserver.notice.entity.NoticeEntity;

import java.time.Instant;

public record NoticeDetail(
        Long noticeId,
        String title,
        String content,
        String authorId,
        Instant createdDateTime,
        Instant updatedDateTime
) {
    public static NoticeDetail of(NoticeEntity entity) {
        return new NoticeDetail(
                entity.getNoticeId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getAuthorId(),
                entity.getCreatedDateTime(),
                entity.getUpdatedDateTime()
        );
    }
}
