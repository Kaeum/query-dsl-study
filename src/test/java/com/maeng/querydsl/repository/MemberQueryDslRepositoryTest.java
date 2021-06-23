package com.maeng.querydsl.repository;

import com.maeng.querydsl.dto.MemberSearchCondition;
import com.maeng.querydsl.dto.MemberTeamDto;
import com.maeng.querydsl.entity.Member;
import com.maeng.querydsl.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberQueryDslRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private MemberQueryDslRepository memberQueryDslRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        em.persist(member);

        Member findMember = memberQueryDslRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberQueryDslRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberQueryDslRepository.findByUsername("member1");
        assertThat(result1).containsExactly(member);
    }

    @Test
    public void searchByBuilderTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition(); condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        List<MemberTeamDto> result = memberQueryDslRepository.searchByBuilder(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchByWhereTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition(); condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        List<MemberTeamDto> result = memberQueryDslRepository.searchByWhere(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }

}