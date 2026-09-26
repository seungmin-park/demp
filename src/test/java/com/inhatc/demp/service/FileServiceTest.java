package com.inhatc.demp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.error.ApiException;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class FileServiceTest {

    private final AmazonS3 amazonS3 = org.mockito.Mockito.mock(AmazonS3.class);
    private final FileService fileService = fileService();

    @ParameterizedTest
    @DisplayName("필수 이미지가 없거나 비어 있으면 400으로 거절한다")
    @MethodSource("missingImages")
    void rejectsMissingImage(MultipartFile image) {
        assertBadRequest(image);
    }

    static Stream<Arguments> missingImages() {
        return Stream.of(
                Arguments.of((MultipartFile) null),
                Arguments.of(new MockMultipartFile("image", "empty.png", "image/png", new byte[0])));
    }

    @Test
    @DisplayName("설정한 5 MiB보다 큰 이미지는 400으로 거절한다")
    void rejectsOversizedImage() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertBadRequest(image);
    }

    @ParameterizedTest
    @DisplayName("확장자·MIME·파일 시그니처가 일치하지 않으면 400으로 거절한다")
    @MethodSource("mismatchedImages")
    void rejectsMismatchedImage(MultipartFile image) {
        assertBadRequest(image);
    }

    @ParameterizedTest
    @DisplayName("JPEG와 PNG 이미지는 검증 후 저장한다")
    @MethodSource("validImages")
    void savesValidImage(MultipartFile image, String expectedExtension) throws IOException {
        UploadFile saved = fileService.save(image);

        assertThat(saved.getUploadFileName()).isEqualTo(image.getOriginalFilename());
        assertThat(saved.getSaveFileName()).endsWith(expectedExtension);
        verify(amazonS3).putObject(any(PutObjectRequest.class));
    }

    static Stream<Arguments> validImages() {
        return Stream.of(
                Arguments.of(new MockMultipartFile("image", "image.jpeg", "image/jpeg",
                        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}), ".jpg"),
                Arguments.of(new MockMultipartFile("image", "image.png", "image/png",
                        new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}), ".png"));
    }

    static Stream<Arguments> mismatchedImages() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
        return Stream.of(
                Arguments.of(new MockMultipartFile("image", "image.png", "image/jpeg", png)),
                Arguments.of(new MockMultipartFile("image", "image.jpg", "image/jpeg", png)),
                Arguments.of(new MockMultipartFile("image", "image.png", "image/png", jpeg)),
                Arguments.of(new MockMultipartFile("image", "image.gif", "image/gif", new byte[] {'G', 'I', 'F'})));
    }

    private void assertBadRequest(MultipartFile image) {
        assertThatThrownBy(() -> fileService.save(image))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(amazonS3);
    }

    private FileService fileService() {
        ImageValidator validator = new ImageValidator();
        ReflectionTestUtils.setField(validator, "maxSizeBytes", 5L * 1024 * 1024);
        FileService service = new FileService(amazonS3, validator);
        ReflectionTestUtils.setField(service, "bucket", "test-bucket");
        return service;
    }
}
