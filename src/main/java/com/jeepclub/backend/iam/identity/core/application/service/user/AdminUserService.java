package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserAdministration;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.core.repository.AdminUserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import com.jeepclub.backend.iam.identity.core.application.command.AdminUserUpdate;
import com.jeepclub.backend.iam.identity.core.application.exception.AdministrativeUserUpdateConflictException;
import com.jeepclub.backend.iam.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.iam.identity.core.domain.model.*;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.identity.core.port.ProfileImagePort;
import com.jeepclub.backend.iam.identity.core.port.UserAuthorizationProtectionPort;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationAdministrationPort;
import com.jeepclub.backend.iam.identity.api.module.spi.UserContactChangePort;
import com.jeepclub.backend.platform.logging.*;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserAdministration userAdministration;
    private final AdminUserQueryRepository adminUserQueryRepository;
    private final Clock clock;
    private final UserRepository users;
    private final UserProfileService profiles;
    private final ProfileImagePort images;
    private final UserAuthorizationProtectionPort protection;
    private final UserAuthenticationAdministrationPort authentication;
    private final List<UserContactChangePort> contactChanges;
    private final SystemLogService audit;

    @Transactional
    public AdminUserResult update(Long userId, Long actorId, AdminUserUpdate update) {
        Objects.requireNonNull(actorId, "actorId is required for administrative audit");
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (update.isEmpty()) return findById(userId);
        String cpf = update.cpf().present() ? User.validateAdministrativeCpf(update.cpf().value()) : user.getCpf();
        String email = User.normalizeEmail(update.email().orElse(user.getEmail()));
        String rg = User.normalizeRg(update.profile().rg().orElse(user.getRg()));
        boolean cpfChanged = !Objects.equals(cpf, user.getCpf());
        boolean emailChanged = !Objects.equals(email, user.getEmail());
        if ((cpfChanged || emailChanged) && !actorId.equals(userId) && protection.hasRootRole(userId)) {
            throw new AdministrativeUserUpdateConflictException();
        }
        if ((cpfChanged && users.existsByCpfAndIdNot(cpf, userId))
                || (emailChanged && email != null && users.existsByEmailAndIdNot(email, userId))
                || (rg != null && !Objects.equals(rg, user.getRg()) && users.existsByRgAndIdNot(rg, userId))) {
            throw new AdministrativeUserUpdateConflictException();
        }
        if (update.profile().profilePhotoStorageKey().present()) images.requireExisting(update.profile().profilePhotoStorageKey().value());
        Map<String, Object> before = fields(user);
        Instant now = Instant.now(clock);
        user.updateAdministratively(update.profile().name().orElse(user.getName()),
                update.profile().birthDate().orElse(user.getBirthDate()), email, cpf, rg,
                update.profile().phoneNumber().orElse(user.getPhoneNumber()),
                update.profile().profilePhotoStorageKey().orElse(user.getProfilePhotoStorageKey()), now);
        Map<String, Object> after = fields(user);
        if (before.equals(after)) return findById(userId);
        try {
            users.saveAndFlush(user);
        } catch (UserConflictException exception) {
            throw new AdministrativeUserUpdateConflictException(exception);
        }
        if (cpfChanged || emailChanged) authentication.identityChanged(userId, cpfChanged, emailChanged, now);
        if (emailChanged) contactChanges.forEach(port -> port.invalidatePendingLinks(userId, now));
        recordChanges(actorId, userId, "IDENTITY_USER_UPDATE", "PATCH", "", before, after, now);
        return findById(userId);
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long userId) { return profiles.get(userId); }

    @Transactional
    public UserProfile replaceProfile(Long userId, Long actorId, WorkProfile work, ResidentialAddress address) {
        Objects.requireNonNull(actorId, "actorId is required for administrative audit");
        users.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Map<String, Object> before = fields(profiles.get(userId));
        UserProfile result = profiles.replace(userId, work, address);
        recordChanges(actorId, userId, "IDENTITY_USER_PROFILE_UPDATE", "PUT", "/profile", before, fields(result), Instant.now(clock));
        return result;
    }

    private void recordChanges(Long actor, Long target, String operation, String method, String suffix,
                               Map<String, Object> before, Map<String, Object> after, Instant now) {
        // One event per changed field: bounded action codes, never field values or PII.
        after.forEach((field, value) -> {
            if (!Objects.equals(before.get(field), value)) audit.recordInTransaction(new SystemLogEvent(actor,
                    operation + ":" + field, method, "/identity/admin/users/" + target + suffix,
                    200, SystemLogOutcome.SUCCESS, 0, null, now));
        });
    }

    private static Map<String, Object> fields(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", user.getName()); result.put("birthDate", user.getBirthDate());
        result.put("cpf", user.getCpf()); result.put("email", user.getEmail()); result.put("rg", user.getRg());
        result.put("phoneNumber", user.getPhoneNumber()); result.put("profilePhotoStorageKey", user.getProfilePhotoStorageKey());
        return result;
    }

    private static Map<String, Object> fields(UserProfile profile) {
        Map<String, Object> result = new LinkedHashMap<>();
        WorkProfile work = profile.workProfile(); ResidentialAddress address = profile.address();
        result.put("occupation", work == null ? null : work.occupation());
        result.put("workplace", work == null ? null : work.workplace());
        result.put("postalCode", address == null ? null : address.postalCode());
        result.put("street", address == null ? null : address.street());
        result.put("number", address == null ? null : address.number());
        result.put("complement", address == null ? null : address.complement());
        result.put("neighborhood", address == null ? null : address.neighborhood());
        result.put("city", address == null ? null : address.city());
        result.put("state", address == null ? null : address.state());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        return adminUserQueryRepository.findAll(filter, fields, pageable);
    }

    @Transactional(readOnly = true)
    public AdminUserResult findById(Long userId) {
        return adminUserQueryRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public AdminUserResult disable(Long userId) {
        userAdministration.disable(userId, Instant.now(clock));
        return findById(userId);
    }

    @Transactional
    public AdminUserResult enable(Long userId) {
        userAdministration.enable(userId, Instant.now(clock));
        return findById(userId);
    }

    @Transactional
    public AdminUserResult archive(Long userId, Long actorId) {
        userAdministration.archive(userId, actorId, Instant.now(clock));
        return findById(userId);
    }

    @Transactional
    public AdminUserResult unarchive(Long userId, Long actorId) {
        userAdministration.unarchive(userId, actorId, Instant.now(clock));
        return findById(userId);
    }
    @Transactional
    public void delete(Long userId, Long actorId) {
        userAdministration.delete(userId, actorId, Instant.now(clock));
    }
    @Transactional
    public AdminUserResult disable(Long id,Long actorId) {
        var result=disable(id);
        audit.recordInTransaction(new SystemLogEvent(actorId,"IDENTITY_USER_DISABLE","PATCH","/identity/admin/users/"+id+"/disable",200,
                SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return result;
    }
    @Transactional
    public AdminUserResult enable(Long id,Long actorId) {
        var result=enable(id);
        audit.recordInTransaction(new SystemLogEvent(actorId,"IDENTITY_USER_ENABLE","PATCH","/identity/admin/users/"+id+"/enable",200,
                SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return result;
    }

}
