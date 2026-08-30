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
import java.util.UUID;

@Slf4j
@Component
public class LocalProfileImageStorage implements ProfileImageStorage {

    private static final String UPLOAD_DIR = "uploads/profile/";

    @Override
    public String store(MultipartFile file, String username) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);

            String filename = username + "_" + UUID.randomUUID() + extension;
            Path savePath = uploadPath.resolve(filename);
            file.transferTo(savePath);

            return "/uploads/profile/" + filename;

        } catch (IOException e) {
            log.error("프로필 이미지 저장 실패. username={}", username, e);
            throw new ClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
