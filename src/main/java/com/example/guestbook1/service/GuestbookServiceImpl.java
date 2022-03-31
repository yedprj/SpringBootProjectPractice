package com.example.guestbook1.service;

import com.example.guestbook1.dto.GuestbookDTO;
import com.example.guestbook1.dto.PageRequestDTO;
import com.example.guestbook1.dto.PageResultDTO;
import com.example.guestbook1.entity.Guestbook;
import com.example.guestbook1.entity.QGuestbook;
import com.example.guestbook1.repository.GuestbookRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

@Service
@Log4j
@RequiredArgsConstructor    // 의존성 자동 주입
public class GuestbookServiceImpl implements GuestbookService{

    private final GuestbookRepository repository;   // 반드시 final로 선언

    @Override
    public Long register(GuestbookDTO dto) {

        log.info("DTO===========================");
        log.info(dto);

        Guestbook entity = dtoToEntity(dto);

        log.info(entity);

        repository.save(entity);

        return entity.getGno();
    }

    @Override
    public PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO requestDTO) {

        Pageable pageable = requestDTO.getPageable(Sort.by("gno").descending());

        BooleanBuilder booleanBuilder = getSearch(requestDTO);  // 검색 조건 처리

        Page<Guestbook> result = repository.findAll(booleanBuilder, pageable);  //Querydsl 사용

        Function<Guestbook, GuestbookDTO> fn = (entity -> entityToDto(entity));

        return new PageResultDTO<>(result, fn);

    }

    @Override
    public GuestbookDTO read(Long gno) {

        Optional<Guestbook> result = repository.findById(gno);
        return result.isPresent()? entityToDto(result.get()): null;
    }

    @Override
    public void remove(Long gno) {

        repository.deleteById(gno);
    }

    @Override
    public void modify(GuestbookDTO dto) {

        // 업데이트 하는 항목 '제목', '내용'

        Optional<Guestbook> result = repository.findById(dto.getGno());

        if (result.isPresent()){

            Guestbook entity = result.get();

            entity.changeTitle(dto.getTitle());
            entity.changeContent(dto.getContent());

            repository.save(entity);
        }
    }
    //Querydsl 처리
    private BooleanBuilder getSearch(PageRequestDTO requestDTO){

        String type = requestDTO.getType();

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        QGuestbook qGuestbook = QGuestbook.guestbook;

        String keyword = requestDTO.getKeyword();

        BooleanExpression expression = qGuestbook.gno.gt(0L); // gno > 0 조건만 생성

        booleanBuilder.and(expression);

        // 검색 조건이 없는 경우
        if (type == null || type.trim().length() == 0) {
            return booleanBuilder;
        }

        //검색 조건을 작성하기
        BooleanBuilder conditionBuilder = new BooleanBuilder();

        if (type.contains("t")){
            conditionBuilder.or(qGuestbook.title.contains(keyword));
        }
        else if (type.contains("c")) {
            conditionBuilder.or(qGuestbook.content.contains(keyword));
        }
        else if (type.contains("w")) {
            conditionBuilder.or(qGuestbook.writer.contains(keyword));
        }

        // 모든 조건 통합
        booleanBuilder.and(conditionBuilder);

        return booleanBuilder;
    }
}
