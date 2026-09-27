package com.inhatc.demp.repository.question;

import com.inhatc.demp.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @EntityGraph(attributePaths = "member")
    @Query("select q from Question q where lower(q.title) like lower(concat('%', :term, '%')) escape '!' or lower(q.member.username) like lower(concat('%', :term, '%')) escape '!'")
    Page<Question> searchForAdmin(String term, Pageable pageable);
}
