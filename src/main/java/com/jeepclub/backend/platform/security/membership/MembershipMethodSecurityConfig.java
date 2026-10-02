package com.jeepclub.backend.platform.security.membership;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;

@Configuration
public class MembershipMethodSecurityConfig {

    private static final int BEFORE_PRE_AUTHORIZE = 150;

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static AuthorizationManagerBeforeMethodInterceptor requiresMembershipInterceptor(
            ObjectProvider<MembershipAccessAuthorization> authorizationProvider
    ) {
        Pointcut pointcut = Pointcuts.union(
                AnnotationMatchingPointcut.forMethodAnnotation(RequiresMembership.class),
                AnnotationMatchingPointcut.forClassAnnotation(RequiresMembership.class)
        );
        AuthorizationManager<MethodInvocation> manager = (authentication, invocation) -> {
            authorizationProvider.getObject().check(authentication.get());
            return new AuthorizationDecision(true);
        };
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(pointcut, manager);
        interceptor.setOrder(BEFORE_PRE_AUTHORIZE);
        return interceptor;
    }
}
