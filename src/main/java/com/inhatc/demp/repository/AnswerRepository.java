package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    public List<Answer> findByQuestion_Id(Long questionId);
}
