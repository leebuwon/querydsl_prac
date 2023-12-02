package study.querydsl.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.domain.member.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    // select m from Member m where m.username = :username
    List<Member> findByUsername(String username);
}
