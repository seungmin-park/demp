package com.inhatc.demp.config.aws;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local & !filesystem")
public class AwsS3Config {
    @Bean
    public S3Client s3Client(@Value("${cloud.aws.credentials.access-key}") String accessKey,
                             @Value("${cloud.aws.credentials.secret-key}") String secretKey,
                             @Value("${cloud.aws.region.static}") String region) {
        return S3Client.builder().region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
