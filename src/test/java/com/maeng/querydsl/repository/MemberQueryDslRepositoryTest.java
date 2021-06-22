package com.maeng.querydsl.repository;

import com.maeng.querydsl.entity.Member;
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

}