package com.maeng.querydsl.repository;

import com.maeng.querydsl.dto.MemberSearchCondition;
import com.maeng.querydsl.dto.MemberTeamDto;
import com.maeng.querydsl.dto.QMemberTeamDto;
import com.maeng.querydsl.entity.Member;
import com.maeng.querydsl.entity.QMember;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.maeng.querydsl.entity.QMember.*;
import static com.maeng.querydsl.entity.QTeam.team;

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

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (StringUtils.hasText(condition.getUsername())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (!ObjectUtils.isEmpty(condition.getAgeGoe())) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (!ObjectUtils.isEmpty(condition.getAgeGoe())) {
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

    public List<MemberTeamDto> searchByWhere(MemberSearchCondition condition) {
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
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }


    private BooleanExpression usernameEq(String username) {
        if(!StringUtils.hasText(username)) {
            return null;
        }

        return member.username.eq(username);
    }

    private BooleanExpression teamNameEq(String teamName) {
        if(!StringUtils.hasText(teamName)) {
            return null;
        }

        return team.name.eq(teamName);
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        if(ObjectUtils.isEmpty(ageGoe)) {
            return null;
        }

        return member.age.goe(ageGoe);
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        if(ObjectUtils.isEmpty(ageLoe)) {
            return null;
        }

        return member.age.loe(ageLoe);
    }
}
