package com.maeng.querydsl.repository;

import com.maeng.querydsl.entity.Member;
import com.maeng.querydsl.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.maeng.querydsl.entity.QMember.*;

@Repository
public class MemberQueryDslRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberQueryDslRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public Optional<Member> findById(Long id) {
        Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(member);
    }

    public List<Member> findAll() {
        return queryFactory
                .selectFrom(member)
                .fetch();

    }
    public List<Member> findByUsername(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }
}
