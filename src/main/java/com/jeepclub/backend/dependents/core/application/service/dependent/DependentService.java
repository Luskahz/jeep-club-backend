package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.exception.*;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentUserPort;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DependentService {

    private final DependentRepository dependentRepository;
    private final DependentUserPort dependentUserPort;
    private final Clock clock;
    private final com.jeepclub.backend.platform.storage.image.ImageMediaCommand mediaCommand;
    private final com.jeepclub.backend.platform.storage.image.PrivateImageMediaQuery privateImages;
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;
    private final java.util.List<com.jeepclub.backend.dependents.api.module.spi.DependentDeletionGuard> deletionGuards;
    private final java.util.List<com.jeepclub.backend.dependents.api.module.spi.DependentLifecycleGuard> lifecycleGuards;
    private final DependentPhotoAuthorization photoAuthorization;

    @Transactional
    public DependentResult create(String name, String cpf, LocalDate birthDate,
            RelationshipType relationshipType, String phoneNumber, Long userId) {
        return create(name,cpf,birthDate,relationshipType,phoneNumber,userId,null);
    }

    @Transactional
    public DependentResult create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            Long userId,
            com.jeepclub.backend.platform.storage.image.ImageUpload photo
    ) {
        assertUserCanOwnDependent(userId);

        String normalizedCpf = normalizeCpf(cpf);

        assertCpfAvailable(normalizedCpf);

        Dependent dependent = Dependent.create(
                name,
                normalizedCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                userId,
                Instant.now(clock)
        );

        if (photo != null) dependent.changePhoto(mediaCommand.storePrivateInTransaction(photo), Instant.now(clock));
        var saved = dependentRepository.save(dependent);
        if (photo != null) record(userId,saved.getId(),"PHOTO_CREATE","POST",201,Instant.now(clock));
        return toResult(saved);
    }

    @Transactional(readOnly = true)
    public List<DependentResult> findAllByUserId(Long userId) {
        return dependentRepository.findAllActiveByUserId(userId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DependentResult findById(
            Long id,
            Long requestingUserId
    ) {
        Dependent dependent = findDependentById(id);

        assertBelongsTo(
                dependent,
                requestingUserId
        );

        return toResult(dependent);
    }

    @Transactional
    public DependentResult update(Long id, String name, String cpf, LocalDate birthDate,
            RelationshipType relationshipType, String phoneNumber, Long requestingUserId) {
        return update(id,name,cpf,birthDate,relationshipType,phoneNumber,requestingUserId,null);
    }

    @Transactional
    public DependentResult update(
            Long id,
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            Long requestingUserId,
            com.jeepclub.backend.platform.storage.image.ImageUpload photo
    ) {
        if (photo != null) assertUserCanOwnDependent(requestingUserId);
        Dependent dependent = findDependentForUpdate(id);

        assertBelongsTo(
                dependent,
                requestingUserId
        );

        if (!dependent.isActive()) throw new com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException("Only active dependents can be edited.");
        String normalizedCpf = normalizeCpf(cpf);

        if (!normalizedCpf.equals(dependent.getCpf())) {
            assertCpfAvailableForUpdate(
                    normalizedCpf,
                    dependent.getId()
            );
        }

        Instant now = Instant.now(clock);
        dependent.update(
                name,
                normalizedCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                now
        );

        if (photo != null) dependent.changePhoto(mediaCommand.storePrivateInTransaction(photo), now);
        var saved = dependentRepository.save(dependent);
        if (photo != null) record(requestingUserId,id,"PHOTO_REPLACE","PUT",200,now);
        return toResult(saved);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public DependentResult disable(Long id, Long requestingUserId) {
        return transition(id, requestingUserId, requestingUserId, "DISABLE");
    }

    @Transactional
    public DependentResult enable(Long id, Long requestingUserId) {
        return transition(id, requestingUserId, requestingUserId, "ENABLE");
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) { delete(id,requestingUserId,requestingUserId); }

    @Transactional
    public void delete(Long id, Long ownerId, Long actorId) {
        dependentUserPort.lockHistoryIdentities(ownerId, actorId);
        var dependent=findDependentForUpdate(id);
        assertBelongsTo(dependent,ownerId);
        if(deletionGuards.stream().anyMatch(guard -> guard.hasRetainedReferences(id)))
            throw new com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException("Dependent has retained Health/Event references.");
        var now=Instant.now(clock);
        dependentRepository.delete(dependent,actorId,now);
        record(actorId,id,"DELETE","DELETE",204,now);
    }

    @Transactional
    public DependentResult replacePhoto(Long id, Long ownerId, com.jeepclub.backend.platform.storage.image.ImageUpload photo) {
        return changePhoto(id,ownerId,ownerId,java.util.Objects.requireNonNull(photo),false);
    }

    @Transactional
    public DependentResult removePhoto(Long id, Long ownerId) { return changePhoto(id,ownerId,ownerId,null,false); }

    @Transactional
    public DependentResult replacePhotoForAdmin(Long id, Long ownerId, Long actorId,
            com.jeepclub.backend.platform.storage.image.ImageUpload photo) {
        return changePhoto(id,ownerId,actorId,java.util.Objects.requireNonNull(photo),true);
    }

    @Transactional
    public DependentResult removePhotoForAdmin(Long id, Long ownerId, Long actorId) { return changePhoto(id,ownerId,actorId,null,true); }

    private DependentResult changePhoto(Long id,Long ownerId,Long actorId,
            com.jeepclub.backend.platform.storage.image.ImageUpload photo,boolean administrative) {
        assertUserCanOwnDependent(ownerId); // User -> Dependent, before upload and every association change.
        Dependent dependent=findDependentForUpdate(id);
        authorizePhoto(dependent,ownerId,administrative);
        if (!dependent.isActive()) throw new com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException("Only active dependents can change photos.");
        String key=photo==null?null:mediaCommand.storePrivateInTransaction(photo);
        Instant now=Instant.now(clock);
        if (!dependent.changePhoto(key,now)) return toResult(dependent);
        var saved=dependentRepository.save(dependent);
        record(actorId,id,photo==null?"PHOTO_REMOVE":"PHOTO_REPLACE",photo==null?"DELETE":"PATCH",200,now);
        return toResult(saved);
    }

    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public com.jeepclub.backend.platform.storage.image.ImageMediaContent photo(Long id,Long ownerId) {
        return readPhoto(id,ownerId,ownerId,false);
    }

    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public com.jeepclub.backend.platform.storage.image.ImageMediaContent photoForAdmin(Long id,Long ownerId,Long actorId) {
        return readPhoto(id,ownerId,actorId,true);
    }

    private com.jeepclub.backend.platform.storage.image.ImageMediaContent readPhoto(Long id,Long ownerId,Long actorId,boolean administrative) {
        com.jeepclub.backend.platform.storage.image.ImageMediaContent content;
        try {
            String authorizedKey=photoAuthorization.authorize(id,ownerId,actorId,administrative);
            // References are immutable; PATCH/DELETE retain the old object. Never
            // re-resolve the current association after authorizing this snapshot.
            content=privateImages.loadPrivate(authorizedKey);
        } catch(RuntimeException failure) {
            recordPhotoRead(actorId,id,photoReadFailureStatus(failure));
            throw failure;
        }
        // REQUIRES_NEW audit commits before any bytes leave this service. Do not
        // turn an audit/commit failure into a successful read or retry as success.
        recordPhotoRead(actorId,id,200);
        return content;
    }

    private int photoReadFailureStatus(RuntimeException failure) {
        if(failure instanceof DependentAccessDeniedException || failure instanceof org.springframework.security.access.AccessDeniedException)return 403;
        if(failure instanceof DependentNotFoundException || failure instanceof com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException)return 404;
        if(failure instanceof com.jeepclub.backend.shared.storage.exception.StorageOperationException)return 503;
        return 500;
    }
    private void recordPhotoRead(Long actor,Long id,int status) {
        audit.recordRequired(new com.jeepclub.backend.platform.logging.SystemLogEvent(actor,"DEPENDENT_PHOTO_READ",
            "GET","/dependents/"+id,status,status==200?com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS:
                status<500?com.jeepclub.backend.platform.logging.SystemLogOutcome.CLIENT_ERROR:com.jeepclub.backend.platform.logging.SystemLogOutcome.SERVER_ERROR,
            0,null,Instant.now(clock)));
    }

    private void authorizePhoto(Dependent dependent,Long ownerId,boolean administrative) {
        if (administrative && !dependent.getUserId().equals(ownerId)) throw new DependentNotFoundException(dependent.getId());
        assertBelongsTo(dependent,ownerId);
    }

    private Dependent findActiveDependentById(Long id) {
        return dependentRepository.findActiveById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );
    }

    private Dependent findDependentById(Long id) {
        return dependentRepository.findById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );
    }

    private void assertUserCanOwnDependent(Long userId) {
        if (!dependentUserPort.existsById(userId)) {
            throw new DependentOwnerNotFoundException(userId);
        }

        if (!dependentUserPort.existsActiveById(userId)) {
            throw new DependentOwnerInactiveException(userId);
        }
    }

    private void assertBelongsTo(
            Dependent dependent,
            Long requestingUserId
    ) {
        if (!dependent.getUserId().equals(requestingUserId)) {
            throw new DependentAccessDeniedException(
                    dependent.getId()
            );
        }
    }

    private void assertCpfAvailable(String cpf) {
        if (dependentUserPort.existsByCpf(cpf)
                || dependentRepository.existsByCpf(cpf)) {

            throw new DependentCpfAlreadyInUseException();
        }
    }

    private void assertCpfAvailableForUpdate(
            String cpf,
            Long dependentId
    ) {
        if (dependentUserPort.existsByCpf(cpf)
                || dependentRepository.existsByCpfAndIdNot(
                cpf,
                dependentId
        )) {

            throw new DependentCpfAlreadyInUseException();
        }
    }

    private DependentResult toResult(Dependent dependent) {
        return DependentResult.from(dependent);
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException(
                    "cpf is required"
            );
        }

        return cpf.replaceAll("\\D", "");
    }

    @Transactional(readOnly=true)
    public List<DependentResult> findAllByUserId(Long ownerId, com.jeepclub.backend.dependents.core.domain.enums.DependentStatus status) {
        return dependentRepository.findAllByUserIdAndStatus(ownerId,status).stream().map(this::toResult).toList();
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public DependentResult archive(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"ARCHIVE"); }
    @Transactional
    public DependentResult unarchive(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"UNARCHIVE"); }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public DependentResult disable(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"DISABLE"); }
    @Transactional
    public DependentResult enable(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"ENABLE"); }

    private DependentResult transition(Long id,Long owner,Long actor,String action) {
        java.util.Objects.requireNonNull(actor,"Lifecycle actor is required");
        var dependent=findDependentForUpdate(id);
        assertBelongsTo(dependent,owner);
        var before=dependent.getStatus();
        var now=Instant.now(clock);
        boolean restrictiveChange=(action.equals("DISABLE") && dependent.isActive())
            || (action.equals("ARCHIVE") && before!=com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ARCHIVED);
        if(restrictiveChange && lifecycleGuards.stream().anyMatch(guard -> guard.hasOperationalParticipation(id,now)))
            throw new com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException("Dependent has operational Event participation.");
        switch(action) {
            case "ARCHIVE" -> dependent.archive(now);
            case "UNARCHIVE" -> dependent.unarchive(now);
            case "DISABLE" -> dependent.disable(now);
            case "ENABLE" -> dependent.enable(now);
            default -> throw new IllegalArgumentException("Unknown dependent lifecycle operation.");
        }
        if(before!=dependent.getStatus()) {
            dependent=dependentRepository.save(dependent);
            record(actor,id,action+":"+before+":"+dependent.getStatus(),"PATCH",200,now);
        }
        return toResult(dependent);
    }
    private Dependent findDependentForUpdate(Long id) {
        return dependentRepository.findByIdForUpdate(id).orElseThrow(() -> new DependentNotFoundException(id));
    }
    private void record(Long actor,Long id,String action,String method,int status,Instant now) {
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actor,"DEPENDENT_"+action,
                method,"/dependents/"+id,status,com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
    }
}
