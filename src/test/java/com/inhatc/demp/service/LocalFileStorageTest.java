package com.inhatc.demp.service;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class LocalFileStorageTest {
    @TempDir Path directory;
    private LocalFileStorage storage() {
        ImageValidator validator = new ImageValidator(); ReflectionTestUtils.setField(validator,"maxSizeBytes",5242880L);
        return new LocalFileStorage(directory.toString(),validator);
    }
    @Test
    @DisplayName("로컬 이미지 저장과 삭제는 실제 파일 내용과 일치한다")
    void savesAndDeletesRealFile() throws Exception {
        byte[] png = {(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a};
        LocalFileStorage storage = storage();
        var file = storage.save(new MockMultipartFile("image","image.png","image/png",png));
        assertThat(directory.resolve(file.getSaveFileName())).exists();
        assertThat(Files.readAllBytes(directory.resolve(file.getSaveFileName()))).isEqualTo(png);
        storage.delete(file.getSaveFileName());
        assertThat(directory.resolve(file.getSaveFileName())).doesNotExist();
    }
    @Test
    @DisplayName("로컬 파일 키의 상위 경로 이동과 잘못된 이미지는 거절한다")
    void rejectsInvalidBoundary() {
        LocalFileStorage storage = storage();
        assertThatThrownBy(() -> storage.delete("../outside.png")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.save(new MockMultipartFile("image","bad.png","image/png",new byte[]{1})))
                .isInstanceOf(com.inhatc.demp.error.ApiException.class);
    }
}
