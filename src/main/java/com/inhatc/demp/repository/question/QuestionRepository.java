package com.inhatc.demp.repository.question;

import com.inhatc.demp.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @Query("update Question q set q.hits = q.hits + 1 where q.id = :id")
    int incrementHits(Long id);

    @EntityGraph(attributePaths = {"member", "questionHashtags.hashtag"})
    @Query("select q from Question q where q.id = :id")
    java.util.Optional<Question> findDetailById(Long id);

    @EntityGraph(attributePaths = "member")
    @Query("select q from Question q where lower(q.title) like lower(concat('%', :term, '%')) escape '!' or lower(q.member.username) like lower(concat('%', :term, '%')) escape '!'")
    Page<Question> searchForAdmin(String term, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Question item where item.id = :id")
    java.util.Optional<Question> findByIdForReaction(Long id);
}
