package com.jeepclub.backend.identity.core.domain.model;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

/** Residual rules exposed by focal PIT; existing registration/profile happy paths are retained. */
class UserMutationBoundaryTest {
    static final Instant CREATED=Instant.parse("2026-10-01T12:00:00Z");
    enum Mutation { DISABLE, ENABLE, UNARCHIVE, EMAIL, PHOTO }
    User user(UserStatus status) {
        return User.reconstitute(7L,"Original",null,"original@example.com","52998224725","123","11999999999",null,
                status,CREATED,status==UserStatus.ACTIVE?null:CREATED,CREATED,
                status==UserStatus.ARCHIVED?CREATED:null);
    }
    void mutate(User user,Mutation mutation,Instant now) {
        switch(mutation) {
            case DISABLE -> user.disable(now);
            case ENABLE -> user.enable(now);
            case UNARCHIVE -> user.unarchive(now);
            case EMAIL -> user.updateEmail("changed@example.com",now);
            case PHOTO -> user.updateProfilePhoto(null,now);
        }
    }
    @ParameterizedTest @EnumSource(Mutation.class)
    void mutationCannotMoveTimeBeforeCreationOrAcceptAbsentTime(Mutation mutation) {
        var status=mutation==Mutation.ENABLE?UserStatus.INACTIVE:
                mutation==Mutation.UNARCHIVE?UserStatus.ARCHIVED:UserStatus.ACTIVE;
        var user=user(status); var before=Snapshot.of(user);
        assertThatIllegalArgumentException().isThrownBy(() -> mutate(user,mutation,CREATED.minusNanos(1)));
        assertThat(Snapshot.of(user)).isEqualTo(before);
        assertThatNullPointerException().isThrownBy(() -> mutate(user,mutation,null));
        assertThat(Snapshot.of(user)).isEqualTo(before);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void archivedIdentityCannotBeEditedThroughProfileOrPhoto(boolean photo) {
        var user=user(UserStatus.ARCHIVED); var before=Snapshot.of(user);
        assertThatThrownBy(() -> {
            if(photo)user.updateProfilePhoto(null,CREATED.plusSeconds(1));
            else user.updateProfile("Changed",null,"456","11888888888",null,CREATED.plusSeconds(1));
        }).isInstanceOf(UserLifecycleConflictException.class);
        assertThat(Snapshot.of(user)).isEqualTo(before);
    }
    @ParameterizedTest @NullSource @ValueSource(longs={0,-1})
    void reconstitutionRequiresAPositiveTechnicalIdentity(Long id) {
        assertThatIllegalArgumentException().isThrownBy(() -> User.reconstitute(id,"Name",null,null,
                "52998224725",null,null,null,UserStatus.ACTIVE,CREATED,null,CREATED));
    }
    @ParameterizedTest @MethodSource("invalidArchival")
    void inconsistentArchivedPersistenceIsRejected(UserStatus status,Instant created,Instant disabled,Instant archived) {
        assertThatIllegalArgumentException().isThrownBy(() -> User.reconstitute(7L,"Name",null,null,
                "52998224725",null,null,null,status,created,disabled,CREATED,archived));
    }
    static Stream<Arguments> invalidArchival() {
        return Stream.of(
                Arguments.of(UserStatus.ARCHIVED,CREATED,CREATED,null),
                Arguments.of(UserStatus.ACTIVE,CREATED,null,CREATED),
                Arguments.of(UserStatus.ARCHIVED,CREATED,CREATED,CREATED.minusNanos(1)),
                Arguments.of(UserStatus.ARCHIVED,null,CREATED,CREATED),
                Arguments.of(UserStatus.ARCHIVED,CREATED,null,CREATED));
    }
    @ParameterizedTest(name="{index}: maximum={1}") @MethodSource("lengthLimits")
    void semanticNormalizationEnforcesInclusiveColumnLimits(Function<String,String> normalize,int maximum,String character) {
        String boundary=character.repeat(maximum);
        assertThat(normalize.apply(boundary)).isEqualTo(boundary);
        assertThatIllegalArgumentException().isThrownBy(() -> normalize.apply(boundary+character));
    }
    static Stream<Arguments> lengthLimits() {
        return Stream.of(
                Arguments.of((Function<String,String>)User::normalizeName,150,"n"),
                Arguments.of((Function<String,String>)User::normalizeCpf,11,"1"),
                Arguments.of((Function<String,String>)User::normalizeRg,20,"1"),
                Arguments.of((Function<String,String>)User::normalizePhoneNumber,20,"1"));
    }
    @Test void emailNormalizationAcceptsItsExactLimitAndRejectsOverflow() {
        String email="a".repeat(64)+"@"+"b".repeat(57)+"."+"c".repeat(53)+".com";
        assertThat(email).hasSize(180);
        assertThat(User.normalizeEmail(email)).isEqualTo(email);
        assertThatIllegalArgumentException().isThrownBy(() -> User.normalizeEmail(email+"m"));
    }
    @Test void administrativeCpfAcceptsAZeroCheckDigitAndStillRejectsChecksumCorruption() {
        assertThat(User.validateAdministrativeCpf("390.533.447-05")).isEqualTo("39053344705");
        assertThatIllegalArgumentException().isThrownBy(() -> User.validateAdministrativeCpf("39053344706"));
    }
    @Test void alreadyUnarchivedIdentityIsANoopWithStableTimestamps() {
        var inactive=user(UserStatus.INACTIVE); var before=Snapshot.of(inactive);
        assertThat(inactive.unarchive(CREATED.plusSeconds(1))).isFalse();
        assertThat(Snapshot.of(inactive)).isEqualTo(before);
    }
    record Snapshot(String name,String email,UserStatus status,Instant disabled,Instant updated,Instant archived,String photo) {
        static Snapshot of(User user) {
            return new Snapshot(user.getName(),user.getEmail(),user.getStatus(),user.getDisabledAt(),
                    user.getUpdatedAt(),user.getArchivedAt(),user.getProfilePhotoStorageKey());
        }
    }
}
