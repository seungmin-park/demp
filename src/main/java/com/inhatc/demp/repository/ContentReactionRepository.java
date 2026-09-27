package com.inhatc.demp.repository;

import com.inhatc.demp.domain.ContentReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReactionRepository extends JpaRepository<ContentReaction, Long> {
    Optional<ContentReaction> findByMember_IdAndQuestion_Id(Long memberId, Long questionId);
    Optional<ContentReaction> findByMember_IdAndAnswer_Id(Long memberId, Long answerId);
    List<ContentReaction> findByMember_IdAndAnswer_Question_Id(Long memberId, Long questionId);
}
