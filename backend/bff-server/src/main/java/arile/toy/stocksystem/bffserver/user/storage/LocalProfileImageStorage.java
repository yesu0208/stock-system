package arile.toy.stocksystem.bffserver.user.storage;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class LocalProfileImageStorage implements ProfileImageStorage {

    private static final String UPLOAD_DIR = "uploads/profile/";

    /** 업로드 허용 확장자 (소문자 비교). 업로드 파일은 /uploads/** 로 공개 제공되므로 html·svg 등 실행 가능한 형식을 차단 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    /** 업로드 허용 Content-Type (클라이언트가 보내는 값이라 보조 검증 용도) */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    @Override
    public String store(MultipartFile file, String username) {
        String extension = validateAndGetExtension(file);

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String filename = username + "_" + UUID.randomUUID() + extension;
            Path savePath = uploadPath.resolve(filename);
            file.transferTo(savePath);

            return "/uploads/profile/" + filename;

        } catch (IOException e) {
            log.error("프로필 이미지 저장 실패. username={}", username, e);
            throw new ClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
        }
    }

    /** 빈 파일·허용되지 않은 확장자·Content-Type을 거부하고, 저장에 쓸 소문자 확장자를 반환 */
    private String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ClientErrorException(HttpStatus.BAD_REQUEST, "업로드할 이미지가 없습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ClientErrorException(HttpStatus.BAD_REQUEST,
                    "jpg, jpeg, png, gif, webp 형식의 이미지만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ClientErrorException(HttpStatus.BAD_REQUEST,
                    "jpg, jpeg, png, gif, webp 형식의 이미지만 업로드할 수 있습니다.");
        }

        return extension;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
