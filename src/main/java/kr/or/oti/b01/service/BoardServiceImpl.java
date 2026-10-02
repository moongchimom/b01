package kr.or.oti.b01.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.domain.BoardImage;
import kr.or.oti.b01.dto.BoardDTO;
import kr.or.oti.b01.dto.BoardListAllDTO;
import kr.or.oti.b01.dto.BoardListReplyCountDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;
import kr.or.oti.b01.repository.BoardRepository;
import kr.or.oti.b01.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoardServiceImpl implements BoardService {

	private final BoardRepository boardRepository;
	private final ModelMapper mapper;
	private final S3Uploader s3Uploader;
	
	public void register(BoardDTO boardDTO) {
		Board board = dtoToEntity(boardDTO);
		boardRepository.save(board);
		
		System.out.println("DEBUG boardDTO ..." + boardDTO);
		System.out.println("DEBUG board ..." + board);
	}
	
	public PageResponseDTO<BoardDTO> getList(PageRequestDTO pageRequestDTO) {
		Pageable pageable = PageRequest.of(pageRequestDTO.getPage()-1, pageRequestDTO.getSize(), Sort.by("bno").descending());
		Page<Board> page = boardRepository.searchAll(pageRequestDTO.getTypes(), pageRequestDTO.getKeyword(), pageable);
		
		List<BoardDTO> dtoList = page.getContent().stream()
				.map(board -> mapper.map(board, BoardDTO.class))
				.collect(Collectors.toList());
		
		int total = (int) page.getTotalElements();
		
		log.info(dtoList.toString());
		log.info("total = " + total);
		
		PageResponseDTO<BoardDTO> result = new PageResponseDTO<>(pageRequestDTO, dtoList, total);
		
		return result;
	}
	
	public BoardDTO get(long bno) {
		Board board = boardRepository.findByIdWithImages(bno)
				.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. bno=" + bno));
		
		return entityToDto(board);
	}

	public void remove(long bno) {
		Board board = boardRepository.findByIdWithImages(bno).orElse(null);
		if (board != null && board.getImageSet() != null) {
			for (BoardImage boardImage : board.getImageSet()) {
				String s3Key = boardImage.getUuid() + "_" + boardImage.getFilename();
				log.info("게시글 삭제에 따른 S3 파일 삭제: {}", s3Key);
				s3Uploader.removeS3File(s3Key);
			}
		}
		boardRepository.deleteById(bno);
	}

	public void modify(BoardDTO boardDTO) {
        Board board = boardRepository.findByIdWithImages(boardDTO.getBno())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. bno=" + boardDTO.getBno()));
        
        board.change(boardDTO.getTitle(), boardDTO.getContent());
        board.clearImages();
        
        if (boardDTO.getFileNames() != null) {
            boardDTO.getFileNames().forEach(fileName -> {
                String pureFileName = fileName;
                if (pureFileName.contains("/")) {
                    pureFileName = pureFileName.substring(pureFileName.lastIndexOf("/") + 1);
                }
                try {
                    pureFileName = URLDecoder.decode(pureFileName, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

                String[] arr = pureFileName.split("_", 2);
                if (arr.length == 2) {
                    board.addImage(arr[0], arr[1]);
                }
            });
        }		
        
        boardRepository.save(board);
    }

	public void removeBatch(List<Long> bnos) {
		log.info("removeBatch bnos: {}", bnos);

		if (bnos == null || bnos.isEmpty()) {
			return;
		}

		for (Long bno : bnos) {
			remove(bno);
		}
	}

	public PageResponseDTO<BoardListReplyCountDTO> listWithReplyCount(PageRequestDTO pageRequestDTO) {
		Pageable pageable = PageRequest.of(pageRequestDTO.getPage()-1, pageRequestDTO.getSize(), Sort.by("bno").descending());
		Page<BoardListReplyCountDTO> page = boardRepository.searchWithReplyCount(pageRequestDTO.getTypes(), pageRequestDTO.getKeyword(), pageable);
		
		return new PageResponseDTO<>(pageRequestDTO, page.getContent(), (int) page.getTotalElements());
	}
	
	public PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO) {
		Pageable pageable = PageRequest.of(pageRequestDTO.getPage()-1, pageRequestDTO.getSize(), Sort.by("bno").descending());
		Page<BoardListAllDTO> page = boardRepository.searchWithAll(pageRequestDTO.getTypes(), pageRequestDTO.getKeyword(), pageable);
		
		return new PageResponseDTO<>(pageRequestDTO, page.getContent(), (int) page.getTotalElements());
	}
}