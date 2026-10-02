package kr.or.oti.b01.dto.upload;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import kr.or.oti.b01.util.S3Uploader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnailator;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class UploadResultDTO {

	private String uuid;

	// 원본 파일명
	private String filename;

	// S3 URL
	private String realFilename;

	// 이미지 여부
	private boolean img;

	// S3 URL 그대로 반환
	public String getLink() {
		return realFilename;
	}

	public UploadResultDTO(String uploadPath, MultipartFile file, S3Uploader s3Uploader) {

		// UUID 생성
		this.uuid = UUID.randomUUID().toString();

		// 원본 파일명
		this.filename = file.getOriginalFilename();

		// S3 업로드 전 실제 파일명
		this.realFilename = this.uuid + "_" + this.filename;

		this.img = false;

		Path path = Paths.get(uploadPath, realFilename);

		try {

			// 로컬 임시 파일 저장
			file.transferTo(path);

			// 이미지인지 확인
			if (file.getContentType().startsWith("image/")) {

				Path thumbFile = Paths.get(uploadPath, "s_" + realFilename);

				// 썸네일 생성
				Thumbnailator.createThumbnail(path.toFile(), thumbFile.toFile(), 200, 200);

				this.img = true;
			}

			// S3 업로드
			this.realFilename = s3Uploader.upload(uploadPath + File.separator + this.uuid + "_" + this.filename);

			log.info("S3에 업로드된 URL = {}", this.realFilename);

		} catch (Exception e) {

			log.error("파일 업로드 오류", e);

		}
	}
}