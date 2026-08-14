package com.jeepclub.backend.shared.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionCatalogTest {

    private static final String APPLICATION_PACKAGE = "com.jeepclub.backend";
    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("['\"]([A-Z][A-Z_]+)['\"]");

    @Test
    void definesEveryPermissionCodeExactlyOnce() {
        PermissionDefinition[] definitions = PermissionDefinition.values();

        assertThat(definitions)
                .extracting(PermissionDefinition::getCode)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrder(PermissionCode.values());

        assertThat(definitions).allSatisfy(definition -> {
            assertThat(definition.name()).isEqualTo(definition.getCode().name());
            assertThat(definition.getDescription()).isNotBlank();
            assertThat(PermissionDefinition.from(definition.getCode())).isSameAs(definition);
        });
    }

    @Test
    void catalogsEveryAuthorityRequiredByControllers() throws ClassNotFoundException {
        Set<String> requiredAuthorities = findControllerAuthorities();
        Set<String> catalogAuthorities = new TreeSet<>();
        Arrays.stream(PermissionCode.values())
                .map(Enum::name)
                .forEach(catalogAuthorities::add);

        assertThat(requiredAuthorities).isNotEmpty();
        assertThat(catalogAuthorities).containsAll(requiredAuthorities);
    }

    private Set<String> findControllerAuthorities() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<String> authorities = new TreeSet<>();
        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(APPLICATION_PACKAGE)) {
            Class<?> controllerClass = Class.forName(beanDefinition.getBeanClassName());
            collectAuthorities(controllerClass, authorities);
            ReflectionUtils.doWithMethods(
                    controllerClass,
                    method -> collectAuthorities(method, authorities)
            );
        }
        return authorities;
    }

    private void collectAuthorities(
            AnnotatedElement element,
            Set<String> authorities
    ) {
        PreAuthorize preAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                element,
                PreAuthorize.class
        );
        if (preAuthorize == null) {
            return;
        }

        Matcher matcher = AUTHORITY_PATTERN.matcher(preAuthorize.value());
        while (matcher.find()) {
            authorities.add(matcher.group(1));
        }
    }
}
