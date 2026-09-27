package com.inhatc.demp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doAnswer;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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

    private final S3Client s3Client = org.mockito.Mockito.mock(S3Client.class);
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
        org.mockito.ArgumentCaptor<PutObjectRequest> request = org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        doAnswer(invocation -> {
            software.amazon.awssdk.core.sync.RequestBody body = invocation.getArgument(1);
            try (java.io.InputStream stream = body.contentStreamProvider().newStream()) {
                assertThat(stream.readAllBytes()).isEqualTo(image.getBytes());
            }
            assertThat(body.contentLength()).isEqualTo(image.getSize());
            return software.amazon.awssdk.services.s3.model.PutObjectResponse.builder().build();
        }).when(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        UploadFile saved = fileService.save(image);

        assertThat(saved.getUploadFileName()).isEqualTo(image.getOriginalFilename());
        assertThat(saved.getSaveFileName()).endsWith(expectedExtension);
        verify(s3Client).putObject(request.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(request.getValue().key()).isEqualTo(saved.getSaveFileName());
        assertThat(request.getValue().contentType()).isEqualTo(image.getContentType());
        assertThat(request.getValue().contentLength()).isEqualTo(image.getSize());
        assertThat(request.getValue().acl()).isEqualTo(software.amazon.awssdk.services.s3.model.ObjectCannedACL.PUBLIC_READ);
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
        verifyNoInteractions(s3Client);
    }

    private FileService fileService() {
        ImageValidator validator = new ImageValidator();
        ReflectionTestUtils.setField(validator, "maxSizeBytes", 5L * 1024 * 1024);
        FileService service = new FileService(s3Client, validator);
        ReflectionTestUtils.setField(service, "bucket", "test-bucket");
        return service;
    }
}
