package com.jeepclub.backend.authorization.core.port;   public boolean belongsToUser(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public boolean referencesRole(Long roleId) {
        return Objects.equals(this.roleId, roleId);
    }

public interface UserIdentityProvider {
    boolean existsById(Long userId);
}
