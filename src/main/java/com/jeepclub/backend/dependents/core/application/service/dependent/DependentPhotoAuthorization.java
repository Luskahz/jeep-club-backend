package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.exception.*;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.port.DependentPhotoAccess;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.security.access.AccessDeniedException;

@Service @RequiredArgsConstructor
public class DependentPhotoAuthorization {
    private final DependentRepository dependents;
    private final DependentPhotoAccess access;

    /** All decisions use one new InnoDB MVCC snapshot. No resource or Identity
     * lock, ORM entity snapshot, physical I/O or audit work inside this transaction. */
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,readOnly=true)
    public String authorize(Long id,Long owner,Long actor,boolean administrative) {
        var reference=dependents.findPhotoReference(id).orElseThrow(() -> new DependentNotFoundException(id));
        if(!access.isActiveActor(actor) || (administrative && !access.canReadAsAdmin(actor)))
            throw new AccessDeniedException("Private photo access denied.");
        if(!reference.ownerId().equals(owner)) {
            if(administrative)throw new DependentNotFoundException(id);
            throw new DependentAccessDeniedException(id);
        }
        if(reference.status()!=DependentStatus.ACTIVE)throw new DependentNotFoundException(id);
        if(reference.storageKey()==null)throw new StorageObjectNotFoundException();
        return reference.storageKey();
    }
}
