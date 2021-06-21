package com.maeng.querydsl;

import com.maeng.querydsl.dto.MemberDto;
import com.maeng.querydsl.dto.QMemberDto;
import com.maeng.querydsl.dto.UserDto;
import com.maeng.querydsl.entity.Member;
import com.maeng.querydsl.entity.QMember;
import com.maeng.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.maeng.querydsl.entity.QMember.*;
import static com.maeng.querydsl.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }

    @Test
    public void findWithJpql() {
        //member1 찾기
        String qlString = "select m " +
                "from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void findWithQuerydsl() {
        //member1 찾기
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))
                )
                .fetch();

        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void 검색조건_복수파라미터로_전달() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        ,member.age.eq(10)
                )
                .fetch();

        assertThat(result.size()).isEqualTo(1);
    }

    /*
    * 회원 정렬 순서
    * 1. 나이 내림차순(desc)
    * 2. 이름 오름차순(asc)
    * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                . selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults(); // count 쿼리가 실행됨. 성능 영

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getResults().get(0).getUsername()).isEqualTo("member3");
        assertThat(result.getResults().get(1).getUsername()).isEqualTo("member2");
    }

    @Test
    public void aggregation() {
        Tuple tuple = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetchOne();

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void groupBy() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .contains("member1", "member2");
    }

    @Test
    public void theta_join() {
        // 연관관계가 없는데 조인하는 경우 - Cartesian 곱으로 가져온 뒤 where 절로 필터링
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
     * 회원과 팀을 조인하는데, 팀 이름이 teamA인 팀만 결과 표시하고 회원은 모두 조회
     */
    @Test
    public void join_using_on() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for(Tuple t : result) {
            System.out.println("t = " + t);
        }
    }

    @Test
    public void join_on_no_relation() {
        // 연관관계 없는 엔티티를 외부 조인
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) // leftJoin(member.team, team)이 아님에 주의
                .on(member.username.eq(team.name))
                .fetch();

        for(Tuple t : result) {
            System.out.println("t = " + t);
        }
    }



    @Test
    public void NoFetchJoin() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoin() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    @Test
    public void subQueryEq() {
        //com.querydsl.jpa.JPAExpressions 사용
        QMember subMember = new QMember("subMember");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(subMember.age.max())
                                .from(subMember))
                )
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    @Test
    public void subQueryGoe() {
        QMember subMember = new QMember("subMember");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(subMember.age.avg())
                                .from(subMember))
                )
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(30,40);
    }

    @Test
    public void subQueryIn() throws Exception {
        QMember subMember = new QMember("subMember");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(subMember.age)
                        .from(subMember)
                        .where(subMember.age.gt(10)))
                )
                .fetch();
        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void projections_basic() {
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .fetch();



        List<Tuple> result2 = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        System.out.println("result = " + result);
        System.out.println("result2 = " + result2);
    }

    @Test
    public void findMemberDto_jpa() {
        List<MemberDto> result = em.createQuery(
                "select new com.maeng.querydsl.dto.MemberDto(m.username, m.age) " +
                        "from Member m"
                , MemberDto.class
        ).getResultList();
    }

    @Test
    public void findMemberDto_querydsl() {
        //property (using setter)
        List<MemberDto> propertyResult = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        //field - 필드 이름으로 찾음.
        List<MemberDto> fieldResult = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        //생성자 사용 - 파라미터 타입으로 찾음.
        List<MemberDto> constructorResult = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findUserDto_querydsl() {
        // 다른 Dto로 조회할 경우. MemberDto는 username,age 인 반면 UserDto는 name,age인 경우.

        //field - as() 사용하여 username -> name.
        queryFactory
                .select(Projections.fields(UserDto.class, member.username.as("name"), member.age))
                .from(member)
                .fetch();

        //생성자의 경우 타입으로 찾기 때문에 문제 없음.
        queryFactory
                .select(Projections.constructor(UserDto.class, member.username, member.age))
                .from(member)
                .fetch();

        // scala query를 사용하는 경우 - ExpressionUtils.as() 사용해야함
        QMember subMember = new QMember("subMember");

        queryFactory
                .select(
                        Projections.fields(
                            UserDto.class
                            , member.username.as("name")
                            , ExpressionUtils.as(
                                    JPAExpressions
                                        .select(subMember.age.max())
                                        .from(subMember), "age")
                        )
                )
                .from(member)
                .fetch();
    }

    @Test
    public void findQMemberDto_Annotation() {
        // 1. MemberDto의 생성자에 @QueryProjection 추가
        // 2. compileQuerydsl 수행

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        //compile 시점에 에러를 잡을 수 있고, 코드가 간결해지는 장점
        //But, View Layer까지 전달되는 MemberDto가 인프라스트럭쳐(QueryDSL)와 강하게 결합되는 단점도 존재함.
    }

    @Test
    public void distinct() {
        queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        // JPQL의 distinct와 동일하게 동작함.
    }

    @Test
    public void dynamic_query_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCondition, Integer ageCondition) {
        BooleanBuilder builder = new BooleanBuilder();

        if(!ObjectUtils.isEmpty(usernameCondition)) {
            builder.and(member.username.eq(usernameCondition));
        }

        if(!ObjectUtils.isEmpty(ageCondition)) {
            builder.and(member.age.eq(ageCondition));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamic_query_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCondition, Integer ageCondition) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCondition), ageEq(ageCondition)) // where 절 안의 결과가 null일 경우에는 무시함.
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCondition) {
        if(ObjectUtils.isEmpty(usernameCondition)) {
            return null;
        }

        return member.username.eq(usernameCondition);
    }

    private BooleanExpression ageEq(Integer ageCondition) {
        if(ObjectUtils.isEmpty(ageCondition)) {
            return null;
        }

        return member.age.eq(ageCondition);
    }

    @Test
    public void bulk() {
        long updateCount = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        long deleteCount = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        // DB로 쿼리를 바로 날리기 때문에 영속성 컨텍스트와 싱크가 안 맞을 수 있음.
        em.flush();
        em.clear();
    }
}
