package arile.toy.stocksystem.bffserver.notice.repository;

import arile.toy.stocksystem.bffserver.notice.entity.NoticeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {
    
    @Query("""
            select n from NoticeEntity n
            where (:cursor is null or n.noticeId < :cursor)
            order by n.noticeId desc
            """)
    List<NoticeEntity> findAllByCursor(@Param("cursor") Long cursor, Pageable pageable);
}
