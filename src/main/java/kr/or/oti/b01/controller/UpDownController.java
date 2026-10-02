package kr.or.oti.b01.controller;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;
import kr.or.oti.b01.dto.upload.UploadFileDTO;
import kr.or.oti.b01.dto.upload.UploadResultDTO;
import kr.or.oti.b01.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UpDownController {
	
	@Value("${spring.servlet.multipart.location}")
	private String uploadPath;
	
	private final S3Uploader s3Uploader;
	
	@ApiOperation(value = "remove 파일", notes = "DELETE 방식으로 파일 삭제 (경로 및 쿼리 파라미터 모두 수용)")
	@DeleteMapping(value = {"/remove/{fileName}", "/remove"})
	public java.util.Map<String, Boolean> removeFile(
			@PathVariable(value = "fileName", required = false) String pathFileName,
			@RequestParam(value = "fileName", required = false) String paramFileName) {
		
		// 1. PathVariable 또는 RequestParam 중 전달된 파일명 선택
		String rawFileName = (paramFileName != null && !paramFileName.isEmpty()) ? paramFileName : pathFileName;
		
		log.info("========== S3 파일 삭제 요청 수신 ==========");
		log.info("원본 수신 파일명: " + rawFileName);
		
		boolean result = false;
		
		if (rawFileName != null && !rawFileName.trim().isEmpty()) {
			try {
				// 2. URL 디코딩 처리 (%EC%B9%98... -> 뭉치.jpg)
				String targetKey = URLDecoder.decode(rawFileName, StandardCharsets.UTF_8.name());
				
				// 3. 풀 URL이나 로컬 경로가 붙어있는 경우 맨 뒤 파일명(UUID_파일명.jpg)만 추출
				if (targetKey.contains("/")) {
					targetKey = targetKey.substring(targetKey.lastIndexOf("/") + 1);
				}
				if (targetKey.contains(File.separator)) {
					targetKey = targetKey.substring(targetKey.lastIndexOf(File.separator) + 1);
				}
				
				log.info("S3 삭제 대상 최종 Key: [" + targetKey + "]");
				
				// 4. S3 버킷에서 실제 파일 삭제
				s3Uploader.removeS3File(targetKey);
				result = true;
				log.info("========== S3 삭제 성공: " + targetKey + " ==========");
				
			} catch (Exception e) {
				log.error("S3 파일 삭제 중 예외 발생: " + e.getMessage(), e);
			}
		} else {
			log.warn("삭제할 파일 이름이 비어있습니다.");
		}
		
		java.util.Map<String, Boolean> resultMap = new java.util.HashMap<>();
		resultMap.put("result", result);
		
		return resultMap;
	}

	@ApiOperation(value = "Upload post", notes = "POST 방식으로 파일 등록")
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public List<UploadResultDTO> upload(UploadFileDTO uploadFileDTO) {
		
		log.info("uploadPath = " + uploadPath);
		log.info("upload = " + uploadFileDTO);
		
		if (uploadFileDTO.getFiles() != null) {
			final List<UploadResultDTO> list = new ArrayList<UploadResultDTO>();
			
			for(MultipartFile file : uploadFileDTO.getFiles()) {
				log.info("원본 파일명 : " + file.getOriginalFilename());
				log.info("파일 유형 : " + file.getContentType());
				log.info("파일 사이즈 : " + file.getSize());
			
				list.add(new UploadResultDTO(uploadPath, file, s3Uploader));
			}
			
			return list;
		}
		return null;
	}
	
	@ApiOperation(value = "view 파일", notes = "Get 방식으로 파일 조회")
	@GetMapping(value = "/view/{filename}")
	public ResponseEntity<Resource> viewFileGet(@PathVariable("filename") String filename) {
		Resource resource = new FileSystemResource(uploadPath + File.separator + filename);
		HttpHeaders headers = new HttpHeaders();
		
		try {
			headers.add("Content-Type", Files.probeContentType(resource.getFile().toPath()));
			headers.add("Content-Length", String.valueOf(resource.getFile().length()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ResponseEntity.ok().headers(headers).body(resource);
	}
}