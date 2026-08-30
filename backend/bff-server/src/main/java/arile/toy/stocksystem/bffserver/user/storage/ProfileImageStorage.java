package arile.toy.stocksystem.bffserver.user.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorage {

    /**
     * 이미지를 저장하고 클라이언트가 접근 가능한 URL을 반환한다.
     *
     * @param file     업로드된 이미지 파일
     * @param username 소유자 (파일명 충돌 방지 및 추적용)
     * @return 저장된 이미지에 접근 가능한 URL (또는 경로)
     */
    String store(MultipartFile file, String username);
}
