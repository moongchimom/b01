package kr.or.oti.b01.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.dto.BoardDTO;
import kr.or.oti.b01.dto.BoardListAllDTO;
import kr.or.oti.b01.dto.BoardListReplyCountDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;

public interface BoardService {
	void register(BoardDTO boardDTO);

	PageResponseDTO<BoardDTO> getList(PageRequestDTO pageRequestDTO);

	BoardDTO get(long tid);

	void remove(long tid);

	void modify(BoardDTO boardDTO);

	void removeBatch(List<Long> bnos);

	PageResponseDTO<BoardListReplyCountDTO> listWithReplyCount(PageRequestDTO pageRequestDTO);

	PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO);

	// DTO -> Entity (등록 및 수정 공통: S3 풀 URL 들어와도 순수 파일명만 DB에 저장)
	default Board dtoToEntity(BoardDTO boardDTO) {
		Board board = Board.builder().bno(boardDTO.getBno()).title(boardDTO.getTitle()).content(boardDTO.getContent())
				.writer(boardDTO.getWriter()).build();

		if (boardDTO.getFileNames() != null) {
			boardDTO.getFileNames().forEach(fileName -> {
				String pureFileName = fileName;
				if (pureFileName.contains("/")) {
					pureFileName = pureFileName.substring(pureFileName.lastIndexOf("/") + 1);
				}
				try {
					pureFileName = URLDecoder.decode(pureFileName, StandardCharsets.UTF_8.name());
				} catch (Exception e) {
					e.printStackTrace();
				}

				String[] arr = pureFileName.split("_", 2);
				if (arr.length == 2) {
					board.addImage(arr[0], arr[1]);
				}
			});
		}
		return board;
	}

	// Entity -> DTO (상세조회 read 시 S3 풀 URL 붙여서 반환)
	default BoardDTO entityToDto(Board board) {
		BoardDTO boardDTO = BoardDTO.builder().bno(board.getBno()).title(board.getTitle()).content(board.getContent())
				.writer(board.getWriter()).regDate(board.getRegDate()).modDate(board.getModDate()).build();

		if (board.getImageSet() != null) {
			List<String> fileNames = board.getImageSet().stream().sorted().map(boardImage -> {
				String s3Key = boardImage.getUuid() + "_" + boardImage.getFilename();
				return "https://oti-s3-moong.s3.ap-northeast-2.amazonaws.com/" + s3Key;
			}).collect(Collectors.toList());

			boardDTO.setFileNames(fileNames);
		}

		return boardDTO;
	}

}
