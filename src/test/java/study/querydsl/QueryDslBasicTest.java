package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.member.dto.MemberDto;
import study.querydsl.domain.member.dto.QMemberDto;
import study.querydsl.domain.member.entity.Member;
import study.querydsl.domain.member.entity.QMember;
import study.querydsl.domain.team.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.domain.member.entity.QMember.*;
import static study.querydsl.domain.team.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(em); // 동시성 문제가 발생하지 않게 설계되어 있음!
        Team teamA = new Team("TeamA");
        Team teamB = new Team("TeamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        em.persist(member1);
        em.persist(member2);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 25, teamB);
        em.persist(member3);
        em.persist(member4);
    }

    /**
     * member.username.eq("member1") // username = 'member1'
     * member.username.ne("member1") //username != 'member1'
     * member.username.eq("member1").not() // username != 'member1'
     * member.username.isNotNull() //이름이 is not null
     * member.age.in(10, 20) // age in (10,20)
     * member.age.notIn(10, 20) // age not in (10, 20)
     * member.age.between(10,30) //between 10, 30
     * member.age.goe(30) // age >= 30
     * member.age.gt(30) // age > 30
     * member.age.loe(30) // age <= 30
     * member.age.lt(30) // age < 30
     * member.username.like("member%") //like 검색
     * member.username.contains("member") // like ‘%member%’ 검색
     * member.username.startsWith("member") //like ‘member%’ 검색
     */

    @Test
    public void startJPQL() {
        // member 1을 찾아라
        Member findMember1 = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl() {
//        QMember m = new QMember("m"); // 같은 테이블을 조인해야하는 경우에만 사용하는 것이 좋다!
//        QMember m = QMember.member; // 이게 더 좋다! -> 근데 이것보다 static import를 사용해서 하는 것이 더 좋다!

        Member findMember1 = jpaQueryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩
                .fetchOne();

        assertThat(findMember1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void searchParam() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"), // and만 있는 경우는 .and보다는 , 으로 연결하는 것이 더 가독성이 좋다!
                        (member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    /**
     * fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
     * fetchOne() : 단 건 조회
     * 결과가 없으면 : null
     * 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
     * fetchFirst() : limit(1).fetchOne()
     * fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
     * fetchCount() : count 쿼리로 변경해서 count 수 조회
     */

    @Test
    public void resultFetchOne() {
        List<Member> fetch = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        Member member1 = jpaQueryFactory.selectFrom(member)
                .fetchOne();

        Member firstMember = jpaQueryFactory
                .selectFrom(member)
                .fetchFirst();

        Long totalCount = jpaQueryFactory
                .select(member.count()) // fetchCount가 deprecated 되기 때문에 내부적으로 count를 해주고 진행하는 것이 좋음
                .from(member)
                .fetchOne();

        System.out.println(totalCount);
    }

    /**
     * 회원 정렬 순서
     * 1, 나이 내림차순
     * 2, 오름차순
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> ageSort = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = ageSort.get(0);
        Member member6 = ageSort.get(1);
        Member memberNull = ageSort.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 시작
                .limit(2) // 끝
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = jpaQueryFactory // QueryDsl의 Tuple다.
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(85);
        assertThat(tuple.get(member.age.max())).isEqualTo(30);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라!
     */
    @Test
    public void group() {
        List<Tuple> fetch = jpaQueryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("TeamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

    }

    /**
     * TeamA에 소속된 모든 회원 찾기
     */
    @Test
    public void join() {
        List<Member> teamA = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("TeamA"))
                .fetch();

        assertThat(teamA).extracting("username").containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> fetch = jpaQueryFactory // 다 조인 해버리고 where 절에서 필터링을 한다.
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원을 모두 조회
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> teamA = jpaQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("TeamA"))
                .fetch();

        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관 관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> fetch = jpaQueryFactory // 다 조인 해버리고 where 절에서 필터링을 한다.
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();


        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member member1 = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded).as("패치조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member member1 = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded).as("패치조인 미적용").isTrue();
    }

    /**
     * 서브 쿼리
     * JPAExpressions 사용
     * 나이가 가장 많은 회원 조회
     */

    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");

//        List<Member> fetch = jpaQueryFactory
//                .selectFrom(member)
//                .where(member.age.eq(
//                        JPAExpressions.select(memberSub.age.max())
//                                .from(memberSub)
//                ))
//                .fetch();

//        assertThat(fetch).extracting("age").containsExactly(30);

        Member member1 = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetchOne();

        assertThat(member1.getAge()).isEqualTo(30);
    }

    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(fetch).extracting("age").containsExactly(30, 25);
    }

    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions.select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(20))
                ))
                .fetch();

        assertThat(fetch).extracting("age").containsExactly(30, 25);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = jpaQueryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> fetch = jpaQueryFactory
                .select(member.age.when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void simpleProjection() {
        List<String> fetch = jpaQueryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s : " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> fetch = jpaQueryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    // 순수 jpql로 작성하면 패키지를 다 적어줘야하기 때문에 지저분할 수 있음
    @Test
    public void findDtoByJpql() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.domain.member.dto.MemberDto(m.username, m.age)" +
                        " from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // setter 주입 (별칭이 다를 경우는 as를 사용해서 해결한다.)
    @Test
    public void findDtoQueryDslSetter() {
        List<MemberDto> fetch = jpaQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username, member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 필드 주입
    @Test
    public void findDtoQueryDslField() {
        List<MemberDto> fetch = jpaQueryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username, member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    // 생성자 주입
    @Test
    public void findDtoQueryDslConstructor() {
        List<MemberDto> fetch = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username, member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // constructor랑 같은 방식인데 컴파일시점에서 오류를 잡아줄 수 있다는 장점이 있다.
    // 하지만
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> fetch = jpaQueryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer age = 10;

        List<Member> result = searchMember1(usernameParam, age);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer age) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }

        if (age != null){
            builder.and(member.age.eq(age));
        }


        return jpaQueryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_whereParam() {
        String usernameParam = "member1";
        Integer age = 10;

        List<Member> result = searchMember2(usernameParam, age);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer age) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(usernameEq(usernameParam), ageEq(age))
//                .where(allEq(usernameParam, age)) // 한번에 하는 것도 가능
                .fetch();
    }

    private BooleanExpression ageEq(Integer age) {
        if (age == null){
            return null;
        }

        return member.age.eq(age);

    }

    private BooleanExpression usernameEq(String usernameParam) {
        if (usernameParam == null){
            return null;
        }

        return member.username.eq(usernameParam);
    }

    private Predicate allEq(String username, Integer age){
        return usernameEq(username).and(ageEq(age));
    }

    @Test
    public void bulkUpdate() {
        long count = jpaQueryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(25))
                .execute();

        // bulk연산이 수행되면 영속성을 한번 초기화하고 진행해주어야 한다. 안해주면 DB에 있는 결과가 들어오지 않고 영속성에 있는 결과가 들어옴
        em.flush();
        em.clear();

        List<Member> fetch = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }

    @Test
    public void bulkAdd() {
        long execute = jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    // delete 수행
    @Test
    public void bulkDelete() {
        long execute = jpaQueryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() {
        List<String> fetch = jpaQueryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }

//
//        String result = jpaQueryFactory
//                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
//                        member.username, "member", "M"))
//                .from(member)
//                .fetchFirst();
//
//        System.out.println(result);
    }

    @Test
    public void sqlFunction2() {
        List<String> fetch = jpaQueryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})",
                        member.username)))
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
}
