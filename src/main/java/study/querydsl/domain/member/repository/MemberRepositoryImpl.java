package study.querydsl.domain.member.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.domain.member.dto.MemberSearchCondition;
import study.querydsl.domain.member.dto.MemberTeamDto;
import study.querydsl.domain.member.dto.QMemberTeamDto;
import study.querydsl.domain.member.entity.Member;

import java.util.List;

import static study.querydsl.domain.member.entity.QMember.member;
import static study.querydsl.domain.team.entity.QTeam.team;

@RequiredArgsConstructor
@Slf4j
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

//    public MemberRepositoryImpl(EntityManager em){
//        this.queryFactory = new JPAQueryFactory(em);
//    }

    @Override
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
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
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        int totalSize = content.size();
        log.info("total size = " + totalSize);
        return new PageImpl<>(content, pageable, totalSize);

    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
