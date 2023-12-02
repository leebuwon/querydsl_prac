package study.querydsl.domain.member.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.domain.member.dto.MemberSearchCondition;
import study.querydsl.domain.member.dto.MemberTeamDto;
import study.querydsl.domain.member.dto.QMemberTeamDto;

import java.util.List;

import static study.querydsl.domain.member.entity.QMember.member;
import static study.querydsl.domain.team.entity.QTeam.team;


/**
 * 만약 query가 너무 핵심 기능이거나 복잡한 로직을 가질 경우는 따로 repository를 만들어서 관리해주면
 * 가독성도 좋아지고 관리하기도 편해진다.
 */
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {
    private final JPAQueryFactory queryFactory;


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
