package com.maeng.querydsl;

import com.maeng.querydsl.dto.*;
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
import org.springframework.util.StringUtils;

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
        //member1 ??????
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
        //member1 ??????
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
    public void ????????????_?????????????????????_??????() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        ,member.age.eq(10)
                )
                .fetch();

        assertThat(result.size()).isEqualTo(1);
    }

    /*
    * ?????? ?????? ??????
    * 1. ?????? ????????????(desc)
    * 2. ?????? ????????????(asc)
    * ??? 2?????? ?????? ????????? ????????? ???????????? ??????(nulls last)
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
                .fetchResults(); // count ????????? ?????????. ?????? ???

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
        // ??????????????? ????????? ???????????? ?????? - Cartesian ????????? ????????? ??? where ?????? ?????????
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
     * ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ?????? ???????????? ????????? ?????? ??????
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
        // ???????????? ?????? ???????????? ?????? ??????
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) // leftJoin(member.team, team)??? ????????? ??????
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
        assertThat(loaded).as("?????? ?????? ?????????").isFalse();
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
        assertThat(loaded).as("?????? ?????? ??????").isTrue();
    }

    @Test
    public void subQueryEq() {
        //com.querydsl.jpa.JPAExpressions ??????
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

        //field - ?????? ???????????? ??????.
        List<MemberDto> fieldResult = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        //????????? ?????? - ???????????? ???????????? ??????.
        List<MemberDto> constructorResult = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findUserDto_querydsl() {
        // ?????? Dto??? ????????? ??????. MemberDto??? username,age ??? ?????? UserDto??? name,age??? ??????.

        //field - as() ???????????? username -> name.
        queryFactory
                .select(Projections.fields(UserDto.class, member.username.as("name"), member.age))
                .from(member)
                .fetch();

        //???????????? ?????? ???????????? ?????? ????????? ?????? ??????.
        queryFactory
                .select(Projections.constructor(UserDto.class, member.username, member.age))
                .from(member)
                .fetch();

        // scala query??? ???????????? ?????? - ExpressionUtils.as() ???????????????
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
        // 1. MemberDto??? ???????????? @QueryProjection ??????
        // 2. compileQuerydsl ??????

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        //compile ????????? ????????? ?????? ??? ??????, ????????? ??????????????? ??????
        //But, View Layer?????? ???????????? MemberDto??? ?????????????????????(QueryDSL)??? ????????? ???????????? ????????? ?????????.
    }

    @Test
    public void distinct() {
        queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        // JPQL??? distinct??? ???????????? ?????????.
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
                .where(usernameEq(usernameCondition), ageEq(ageCondition)) // where ??? ?????? ????????? null??? ???????????? ?????????.
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
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

        long deleteCount = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        // DB??? ????????? ?????? ????????? ????????? ????????? ??????????????? ????????? ??? ?????? ??? ??????.
        em.flush();
        em.clear();
    }


}
