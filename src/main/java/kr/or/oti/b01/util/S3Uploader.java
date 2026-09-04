package kr.or.oti.b01.util;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {
    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    public String bucket;

    public String upload(String filepath) throws RuntimeException {
        File targetFile = new File(filepath);

        // S3에 로컬 파일을 업로드합니다
        String uploadImageUrl = putS3(targetFile, targetFile.getName());

        // S3에 업로드 된 로컬 파일을 삭제한다.
        removeOriginalFile(targetFile);

        // S3에 파일을 업로드 URL 경로를 리턴합니다
        return uploadImageUrl;
    }

    private String putS3(File targetFile, String name) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, name, targetFile)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        return amazonS3Client.getUrl(bucket, name).toString();
    }

    private void removeOriginalFile(File targetFile) {
        if (targetFile.exists() && targetFile.delete()) {
            log.info("로컬 임시 파일이 정상 삭제되었습니다.");
            return;
        }
        log.info("로컬 임시 파일 삭제 실패했습니다.");
    }

    // S3에 업로드된 파일을 삭제한다
    public void removeS3File(String fileName) {
        try {
            // 1. URL 디코딩 (%EC%B9%98... -> 뭉치.jpg)
            String targetKey = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());

            // 2. 전체 URL이나 슬래시(/)가 포함된 경우 순수 파일명(UUID_파일명.jpg)만 추출
            if (targetKey.contains("/")) {
                targetKey = targetKey.substring(targetKey.lastIndexOf("/") + 1);
            }

            log.info(">>> [AWS S3 삭제 시도] 버킷명: {}, 타겟 Key: {}", bucket, targetKey);

            // 3. S3 실제 삭제 요청 실행
            amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, targetKey));

            log.info(">>> [AWS S3 삭제 성공] 버킷에서 파일이 제거되었습니다: {}", targetKey);
        } catch (Exception e) {
            log.error(">>> [AWS S3 삭제 에러] 예외 발생: {}", e.getMessage(), e);
        }
    }
}