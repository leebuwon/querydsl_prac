package study.querydsl.domain.member.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.domain.member.dto.MemberSearchCondition;
import study.querydsl.domain.member.dto.MemberTeamDto;
import study.querydsl.domain.member.dto.QMemberDto;
import study.querydsl.domain.member.dto.QMemberTeamDto;
import study.querydsl.domain.member.entity.Member;
import study.querydsl.domain.member.entity.QMember;
import study.querydsl.domain.team.entity.QTeam;

import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.domain.member.entity.QMember.*;
import static study.querydsl.domain.team.entity.QTeam.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;
    public void save(Member member) {
        em.persist(member);
    }
    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    // Query Dsl로 수정
    public List<Member> findAll_QueryDsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }
    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_QueryDsl(String username){
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchBuilder(MemberSearchCondition condition){
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())){
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())){
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null){
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe() != null){
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                // where 방식이 더 좋은 이유는 무엇일까? 재사용성이 좋아진다. 이게 가장 큰 객체지향의 특징인데 이걸 이용할 수 있음!
                .where(usernameEq(condition.getUsername()), teamNameEq(condition.getTeamName()), ageGoe(condition.getAgeGoe()), ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private Predicate ageLoe(Integer ageLoe) {
        if (ageLoe == null){
            return null;
        }

        return member.age.loe(ageLoe);
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        if (ageGoe == null){
            return null;
        }

        return member.age.goe(ageGoe);
    }

    private BooleanExpression teamNameEq(String teamName) {
        if (teamName == null){
            return null;
        }

        return team.name.eq(teamName);
    }

    private BooleanExpression usernameEq(String username) {
        if (username == null){
            return null;
        }

        return member.username.eq(username);
    }
}
