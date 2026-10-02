package kr.or.oti.b01.dto;

import java.time.LocalDate;
import java.util.Arrays;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {
	@Builder.Default
	@Min(value = 1)
	@Positive
	private int page = 1;
	
	@Builder.Default
	@Min(value = 10)
	@Max(value = 100)
	@Positive
	private int size = 10;
		
	private String link;
	
	private String [] types;
	private String keyword;
	private boolean finished;
	private LocalDate from;
	private LocalDate to;
	
	
	public int getSkip() {
		return (page - 1) * size;
	}
	
	public String getLink() {
		if (link == null) {
			StringBuilder builder = new StringBuilder();
			
			//URL에 사용하는 파라미터 조합
			// /page=?&size=? 형태의 URL
			builder.append("page=").append(page)
				.append("&size=").append(size);
			
			//types가 null이 아니면 배열을 순회
			// &types=t&types=c 형태로 붙임
			if (types != null) {
				for (String type : types) {
					builder.append("&types=").append(type);		
				}
			}
			
			//검색어 및 필터 조건 검사
			
			//검색어
			if (keyword != null && keyword.length() > 0) {
				builder.append("&keyword=").append(keyword);	
			}
			
			//기간이 끝난 정책을 필터링 하는 검색 조건
			// [V]마감된 정책 제외 , [V]진행중인 공고만 보기
			if (finished) {
				builder.append("&finished=on");	
			}
			
			//접수기간 조건 검색
			if (from != null) {
				builder.append("&from=").append(from);	
			}
			
			//접수기간 조건 검색
			if (to != null) {
				builder.append("&to=").append(to);	
			}
			
			// builder를 통해 완성된 URL을 toString처리 후 반환
			link = builder.toString();
		}
		return link;
	}
	
	// 사용자가 페이지를 이동하거나 새로고침 했을 때
	// 화면의 체크박스의 체크 상태 유지
	public boolean isCheckType(String type) {
		if (types != null) {
//			for (String item : types) {
//				if (item.equals(type)) return true;	
//			}
			//문자열 배열인 types를 Stream객체로 변환
			//Stream 요소중 매개변수 type과 문자열이 일치하는지 검사
			//일치하는 항목이 나오면 탐색을 멈추고 true 반환
			return Arrays.stream(types).anyMatch(type::equals);
		}
		return false;
	}
}
