package com.inhatc.demp.repository.question;

import com.inhatc.demp.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
