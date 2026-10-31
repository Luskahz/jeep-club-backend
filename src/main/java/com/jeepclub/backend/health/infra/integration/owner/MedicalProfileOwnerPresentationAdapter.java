package com.jeepclub.backend.health.infra.integration.owner;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerInactiveException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerPresentationQuery;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.*;

@Component
@RequiredArgsConstructor
public class MedicalProfileOwnerPresentationAdapter implements MedicalProfileOwnerPresentationQuery {
    private static final int BATCH_SIZE = 500;
    private final UserQuery users;
    private final DependentsQuery dependents;

    @Override
    public Map<Owner, Names> resolve(Collection<Owner> owners) {
        if (owners.isEmpty()) return Map.of();
        Set<Long> userIds = new HashSet<>();
        Set<Long> dependentIds = new HashSet<>();
        for (Owner owner : owners) {
            if (owner.type() == USER) userIds.add(owner.id());
            else dependentIds.add(owner.id());
        }
        Map<Long, DependentsQuery.Details> dependentDetails = new HashMap<>();
        for (var details : batches(dependentIds, dependents::findDetailsByIds)) {
            dependentDetails.put(details.id(), details);
            if (details.userId() != null) userIds.add(details.userId());
        }
        Map<Long, UserDetails> userDetails = new HashMap<>();
        for (var details : batches(userIds, users::findByIds)) userDetails.put(details.id(), details);

        Map<Owner, Names> result = new HashMap<>();
        for (Owner owner : owners) {
            if (owner.type() == USER) {
                UserDetails user = userDetails.get(owner.id());
                if (user == null) throw new MedicalProfileOwnerNotFoundException(USER, owner.id());
                if (!user.administrativelyActive()) throw new MedicalProfileOwnerInactiveException(USER, owner.id());
                result.put(owner, new Names(user.name(), null));
            } else {
                var dependent = dependentDetails.get(owner.id());
                if (dependent == null) throw new MedicalProfileOwnerNotFoundException(DEPENDENT, owner.id());
                if (!"ACTIVE".equals(dependent.status())) throw new MedicalProfileOwnerInactiveException(DEPENDENT, owner.id());
                UserDetails responsible = userDetails.get(dependent.userId());
                // Broken registration cannot become a successful, partially identified clinical response.
                if (responsible == null) throw new MedicalProfileOwnerNotFoundException(DEPENDENT, owner.id());
                // The responsible user's status does not change the dependent's existing eligibility rule.
                result.put(owner, new Names(dependent.name(), responsible.name()));
            }
        }
        return Map.copyOf(result);
    }

    private static <T> List<T> batches(Set<Long> ids, Function<Collection<Long>, List<T>> query) {
        List<Long> orderedIds = new ArrayList<>(ids);
        List<T> result = new ArrayList<>();
        for (int from = 0; from < orderedIds.size(); from += BATCH_SIZE) {
            result.addAll(query.apply(orderedIds.subList(from, Math.min(from + BATCH_SIZE, orderedIds.size()))));
        }
        return result;
    }
}
