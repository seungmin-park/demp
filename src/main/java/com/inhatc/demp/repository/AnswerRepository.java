package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @EntityGraph(attributePaths = "member")
    List<Answer> findByQuestion_Id(Long questionId);
    @EntityGraph(attributePaths = {"member", "question"})
    @Query("select q from Answer q where lower(q.question.title) like lower(concat('%', :term, '%')) escape '!' or lower(q.member.username) like lower(concat('%', :term, '%')) escape '!'")
    Page<Answer> searchForAdmin(String term, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Answer item where item.id = :id")
    java.util.Optional<Answer> findByIdForReaction(Long id);
}
