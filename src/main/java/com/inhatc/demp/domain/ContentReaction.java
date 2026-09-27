package com.inhatc.demp.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_reaction_member_question", columnNames = {"member_id", "question_id"}),
        @UniqueConstraint(name = "uk_reaction_member_answer", columnNames = {"member_id", "answer_id"})})
@Check(constraints = "(question_id is not null and answer_id is null) or (question_id is null and answer_id is not null)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentReaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Question question;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Answer answer;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private ReactionType reaction;

    @Builder
    private ContentReaction(Member member, Question question, Answer answer, ReactionType reaction) {
        if (member == null || (question == null) == (answer == null))
            throw new IllegalArgumentException("회원과 하나의 반응 대상이 필요합니다.");
        this.member = member; this.question = question; this.answer = answer;
        changeTo(reaction);
    }

    public void changeTo(ReactionType reaction) {
        this.reaction = java.util.Objects.requireNonNull(reaction);
    }
}
