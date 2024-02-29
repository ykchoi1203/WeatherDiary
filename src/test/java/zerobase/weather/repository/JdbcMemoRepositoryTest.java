package zerobase.weather.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Memo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional  //db test 할 때 사용하는 어노테이션
class JdbcMemoRepositoryTest {
    @Autowired
    JdbcMemoRepository jdbcMemoRepository;

    @Test
    void insertMemo() {
        // given
        Memo newMemo = new Memo(2, "두번째 메모입니다.");

        // when
        jdbcMemoRepository.save(newMemo);

        // then
        Optional<Memo> result = jdbcMemoRepository.findById(2);
        assertEquals(result.get().getText(), "두번째 메모입니다.");

    }

    @Test
    void findAllMemoTest() {
        // given
        List<Memo> memoList = jdbcMemoRepository.findAll();
        System.out.println(memoList);
        // when

        // then
        assertNotNull(memoList);

    }

}