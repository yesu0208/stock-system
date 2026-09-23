package arile.toy.stocksystem.bffserver.user.storage;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

class LocalProfileImageStorageTest {

    private static final Path UPLOAD_DIR = Paths.get("uploads/profile/");
    private static final byte[] CONTENT = {1, 2, 3};

    private final LocalProfileImageStorage storage = new LocalProfileImageStorage();
    private final List<Path> createdFiles = new ArrayList<>();

    @AfterEach
    void cleanUp() throws IOException {
        for (Path path : createdFiles) {
            Files.deleteIfExists(path);
        }
    }

    /** 반환된 URL("/uploads/profile/xxx")을 실제 파일 경로로 바꾸고 정리 대상으로 등록 */
    private Path trackSavedFile(String url) {
        Path path = Paths.get(url.substring(1));
        createdFiles.add(path);
        return path;
    }

    private static MockMultipartFile image(String filename, String contentType) {
        return new MockMultipartFile("image", filename, contentType, CONTENT);
    }

    private static void assertNoFileSavedFor(String username) throws IOException {
        if (!Files.exists(UPLOAD_DIR)) {
            return;
        }
        try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .noneMatch(name -> name.startsWith(username + "_"));
        }
    }

    private static void assertBadRequest(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ClientErrorException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ===================== 저장 성공 =====================

    @ParameterizedTest(name = "{0} ({1})")
    @CsvSource({
            "photo.jpg, image/jpeg, .jpg",
            "photo.jpeg, image/jpeg, .jpeg",
            "photo.png, image/png, .png",
            "photo.gif, image/gif, .gif",
            "photo.webp, image/webp, .webp"
    })
    @DisplayName("허용된 이미지 형식이면 사용자명_UUID 파일명으로 저장하고 공개 URL을 반환한다")
    void store_allowedFormats(String filename, String contentType, String extension) throws IOException {
        String url = storage.store(image(filename, contentType), "user1");

        assertThat(url).matches("/uploads/profile/user1_[0-9a-f-]{36}" + Pattern.quote(extension));
        Path saved = trackSavedFile(url);
        assertThat(Files.readAllBytes(saved)).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("확장자와 Content-Type의 대소문자는 구분하지 않고, 저장할 때는 소문자 확장자를 쓴다")
    void store_caseInsensitive() {
        String url = storage.store(image("PHOTO.PNG", "IMAGE/PNG"), "user1");

        trackSavedFile(url);
        assertThat(url).endsWith(".png");
    }

    @Test
    @DisplayName("같은 파일을 여러 번 올려도 파일명이 겹치지 않는다")
    void store_uniqueFilenames() {
        String first = storage.store(image("a.png", "image/png"), "user1");
        String second = storage.store(image("a.png", "image/png"), "user1");

        trackSavedFile(first);
        trackSavedFile(second);
        assertThat(first).isNotEqualTo(second);
    }

    // ===================== 거부 =====================

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"evil.html", "evil.svg", "evil.exe", "evil.png.html", "noextension", "evil."})
    @DisplayName("허용되지 않은 확장자는 Content-Type이 이미지여도 400으로 거부하고 저장하지 않는다")
    void store_disallowedExtension(String filename) throws IOException {
        String username = "ext" + Math.abs(filename.hashCode());

        assertBadRequest(() -> storage.store(image(filename, "image/png"), username));

        assertNoFileSavedFor(username);
    }

    @ParameterizedTest(name = "파일명 없음")
    @NullSource
    @DisplayName("파일명이 없으면 400으로 거부한다")
    void store_nullFilename(String filename) throws IOException {
        assertBadRequest(() -> storage.store(image(filename, "image/png"), "nullname"));

        assertNoFileSavedFor("nullname");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"text/html", "image/svg+xml", "application/octet-stream"})
    @DisplayName("허용되지 않은 Content-Type은 확장자가 이미지여도 400으로 거부한다")
    void store_disallowedContentType(String contentType) throws IOException {
        String username = "ct" + Math.abs(contentType.hashCode());

        assertBadRequest(() -> storage.store(image("photo.png", contentType), username));

        assertNoFileSavedFor(username);
    }

    @Test
    @DisplayName("Content-Type이 없으면 400으로 거부한다")
    void store_nullContentType() throws IOException {
        assertBadRequest(() -> storage.store(image("photo.png", null), "noctype"));

        assertNoFileSavedFor("noctype");
    }

    @Test
    @DisplayName("빈 파일이나 null이면 400으로 거부한다")
    void store_emptyFile() {
        assertBadRequest(() -> storage.store(new MockMultipartFile("image", "a.png", "image/png", new byte[0]), "empty"));
        assertBadRequest(() -> storage.store(null, "empty"));
    }

    // ===================== 저장 실패 =====================

    @Test
    @DisplayName("디스크 저장 중 IOException이 나면 500 ClientErrorException으로 바꿔 던진다")
    void store_ioFailure() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getOriginalFilename()).willReturn("photo.png");
        given(file.getContentType()).willReturn("image/png");
        willThrow(new IOException("disk full")).given(file).transferTo(any(Path.class));

        assertThatThrownBy(() -> storage.store(file, "user1"))
                .isInstanceOfSatisfying(ClientErrorException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(e.getMessage()).isEqualTo("파일 업로드에 실패했습니다.");
                });
    }
}
