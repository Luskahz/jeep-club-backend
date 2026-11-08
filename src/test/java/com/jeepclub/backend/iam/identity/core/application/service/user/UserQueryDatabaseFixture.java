package com.jeepclub.backend.iam.identity.core.application.service.user;

/** Uses the real Identity query implementation and repository inside the caller's transaction. */
public final class UserQueryDatabaseFixture {
    private UserQueryDatabaseFixture() {}
    public static com.jeepclub.backend.iam.identity.api.module.UserQuery query(
            com.jeepclub.backend.iam.identity.core.repository.UserRepository repository) {
        return new UserQueryService(repository);
    }
}
