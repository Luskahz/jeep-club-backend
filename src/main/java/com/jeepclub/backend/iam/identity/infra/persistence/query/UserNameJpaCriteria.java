package com.jeepclub.backend.iam.identity.infra.persistence.query;

import com.jeepclub.backend.iam.identity.api.module.UserNameCriteria;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UserNameJpaCriteria implements UserNameCriteria {
    @Override
    public Predicate matches(CriteriaBuilder builder, CommonAbstractCriteria query, Expression<Long> userId, String name) {
        if (name == null || name.isBlank()) return builder.disjunction();
        if (name.length() > 150) throw new IllegalArgumentException("Maximum name search length: 150");
        var match = query.subquery(Integer.class);
        var user = match.from(UserEntity.class);
        String pattern = "%" + name.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        match.select(builder.literal(1)).where(builder.equal(user.get("id"), userId),
                builder.like(builder.lower(user.get("name")), pattern, '!'));
        return builder.exists(match);
    }
}
