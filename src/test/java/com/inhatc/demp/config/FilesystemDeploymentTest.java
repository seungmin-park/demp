package com.inhatc.demp.config;

import com.inhatc.demp.config.aws.AwsS3Config;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.service.FileService;
import com.inhatc.demp.service.FileStorage;
import com.inhatc.demp.service.ImageValidator;
import com.inhatc.demp.service.LocalFileStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

class FilesystemDeploymentTest {
    @TempDir
    Path directory;

    @Test
    @DisplayName("파일시스템 배포는 S3 대신 영속 디렉터리에 이미지를 저장한다")
    void filesystemProfileSelectsPersistentStorage() {
        new ApplicationContextRunner()
                .withUserConfiguration(LocalFileStorage.class, LocalImageConfiguration.class,
                        ImageValidator.class, FileService.class, AwsS3Config.class)
                .withPropertyValues("spring.profiles.active=filesystem", "app.local.upload-dir=" + directory,
                        "cloud.aws.credentials.access-key=test-unused", "cloud.aws.credentials.secret-key=test-unused",
                        "cloud.aws.region.static=ap-northeast-2", "cloud.aws.s3.bucket=test-unused")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(LocalFileStorage.class)
                            .hasSingleBean(FileStorage.class).doesNotHaveBean(S3Client.class);
                    FileStorage storage = context.getBean(FileStorage.class);
                    byte[] bytes = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
                    UploadFile uploaded = storage.save(new MockMultipartFile("file", "image.png", "image/png", bytes));
                    assertThat(Files.readAllBytes(directory.resolve(uploaded.getSaveFileName()))).isEqualTo(bytes);
                    storage.delete(uploaded.getSaveFileName());
                    assertThat(directory.resolve(uploaded.getSaveFileName())).doesNotExist();
                });
    }
}
