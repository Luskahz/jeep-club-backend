package com.jeepclub.backend.dependents.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import com.jeepclub.backend.dependents.infra.persistence.jpa.*;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.authentication.core.repository.*;
import com.jeepclub.backend.iam.authentication.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.platform.security.jwt.*;
import com.jeepclub.backend.platform.storage.image.*;
import com.jeepclub.backend.platform.logging.*;
import com.jeepclub.backend.shared.authorization.*;
import com.jeepclub.backend.shared.storage.*;
import com.jeepclub.backend.shared.storage.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Committed HTTP transactions, real local storage and real Identity/Authorization persistence. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DependentPhotoIntegrationTest {
    @TempDir static Path PHOTO_ROOT;
    @DynamicPropertySource static void photos(DynamicPropertyRegistry registry) {
        registry.add("storage.local.root-directory",() -> PHOTO_ROOT.toString());
    }
    static final byte[] PNG={(byte)137,80,78,71,13,10,26,10,1};
    static final AtomicInteger CPFS=new AtomicInteger(840000000);
    @Autowired MockMvc mvc;
    @Autowired DependentService dependents;
    @Autowired DependentJpaRepository jpa;
    @Autowired DependentHistoryJpaRepository history;
    @Autowired UserRepository users;
    @Autowired AuthenticationAccountRepository accounts;
    @Autowired SessionRepository sessions;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired UserRoleRepository userRoles;
    @Autowired TransactionTemplate transactions;
    @Autowired Clock clock;
    @Autowired com.jeepclub.backend.dependents.api.module.DependentsQuery publicDependents;
    @Autowired com.jeepclub.backend.iam.identity.api.module.UserAdministration identities;
    @MockitoBean JwtTokenParser parser;
    @MockitoSpyBean FileStorage storage;
    @MockitoSpyBean DependentRepository repository;
    @MockitoSpyBean SystemLogService audit;
    Long owner,other,manager,reader;
    String ownerToken,otherToken,managerToken,readerToken;

    @BeforeEach void actors() {
        owner=user();other=user();manager=user();reader=user();
        grant(manager,PermissionCode.DEPENDENTS_DEPENDENT_READ,PermissionCode.DEPENDENTS_DEPENDENT_PHOTO_UPDATE,PermissionCode.DEPENDENTS_DEPENDENT_EXPORT);
        grant(reader,PermissionCode.DEPENDENTS_DEPENDENT_READ);
        ownerToken=token(owner);otherToken=token(other);managerToken=token(manager);readerToken=token(reader);
    }

    @Test void jsonAndMultipartWithoutPhotoRemainCompatible() throws Exception {
        mvc.perform(post("/dependents").header("Authorization",ownerToken).contentType(MediaType.APPLICATION_JSON).content(data(cpf(),"No photo JSON")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.photoUrl").doesNotExist());
        create(ownerToken,cpf(),null).andExpect(status().isCreated()).andExpect(jsonPath("$.photoUrl").doesNotExist());
        assertThat(jpa.findAllByUserId(owner)).hasSize(2).allSatisfy(d -> assertThat(d.getPhotoStorageKey()).isNull());
        verify(storage,never()).store(any(),anyString());
    }

    @Test void requestReadFailurePreservesPrivatePhotoAndRegistrationOnEveryUploadRoute() throws Exception {
        String cpf = cpf(); Long id = id(create(ownerToken, cpf, PNG).andExpect(status().isCreated()));
        String original = key(id); long beforeRows = jpa.count(), beforeFiles = files();
        var prior = jpa.findById(id).orElseThrow();
        clearInvocations(storage, repository);
        var requests = List.of(
                multipart("/dependents").file(structured(data(cpf(), "Failed creation"))).header("Authorization", ownerToken),
                multipart(org.springframework.http.HttpMethod.PUT, "/dependents/{id}", id).file(structured(data(cpf, "Failed update"))).header("Authorization", ownerToken),
                multipart(org.springframework.http.HttpMethod.PATCH, "/dependents/{id}/photo", id).header("Authorization", ownerToken),
                multipart(org.springframework.http.HttpMethod.PATCH, "/users/{owner}/dependents/{id}/photo", owner, id).header("Authorization", managerToken));
        for (var request : requests) {
            var broken = spy(new MockMultipartFile("photo", "sensitive-filename.png", "image/png", PNG));
            doThrow(new java.io.IOException("sensitive-path-storageKey-bytes")).when(broken).getBytes();
            mvc.perform(request.file(broken)).andExpect(status().isInternalServerError())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_READ_FAILED")).andExpect(jsonPath("$.status").value(500))
                    .andExpect(r -> assertThat(r.getResponse().getContentAsString()).doesNotContain("sensitive-", original, "IOException", "stackTrace"));
            assertThat(jpa.count()).isEqualTo(beforeRows); assertThat(files()).isEqualTo(beforeFiles);
            assertThat(jpa.findById(id).orElseThrow()).satisfies(d -> {
                assertThat(d.getName()).isEqualTo(prior.getName()); assertThat(d.getUpdatedAt()).isEqualTo(prior.getUpdatedAt());
                assertThat(d.getPhotoStorageKey()).isEqualTo(original);
            });
            verify(repository, never()).save(any()); verifyNoInteractions(storage);
        }
        assertThat(storage.load(original).content()).containsExactly(PNG);
    }

    @Test void creationStoresPrivateReferenceAndOnlyAuthorizedBusinessRoutesResolveIt() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String key=key(id);
        assertThat(key).startsWith("private-images/");assertThat(storage.load(key).content()).containsExactly(PNG);
        mvc.perform(get("/dependents/{id}",id).header("Authorization",ownerToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("/dependents/"+id+"/photo"))
                .andExpect(jsonPath("$.photoStorageKey").doesNotExist()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(key))));
        mvc.perform(get("/dependents").header("Authorization",ownerToken)).andExpect(status().isOk()).andExpect(jsonPath("$[0].photoUrl").isNotEmpty());
        mvc.perform(get("/media/images").param("key",key).header("Authorization",otherToken)).andExpect(status().isBadRequest());
        mvc.perform(get("/dependents/{id}/photo",id).header("Authorization",otherToken)).andExpect(status().isForbidden());
        read(id,ownerToken).andExpect(status().isOk()).andExpect(content().bytes(PNG)).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Content-Disposition","inline; filename=\"dependent-photo.png\""));
        verify(audit).recordInTransaction(argThat(e -> e.action().equals("DEPENDENT_PHOTO_READ")&&e.actorId().equals(owner)));
    }

    @Test void adminReadAndPhotoMaintenanceHaveDistinctExplicitAuthoritiesAndValidateBothIds() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String path="/users/"+owner+"/dependents/"+id+"/photo";
        mvc.perform(get(path).header("Authorization",readerToken)).andExpect(status().isOk()).andExpect(content().bytes(PNG));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PATCH,path).file(photo(PNG)).header("Authorization",readerToken)).andExpect(status().isForbidden());
        mvc.perform(delete(path).header("Authorization",readerToken)).andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization",otherToken)).andExpect(status().isForbidden());
        mvc.perform(get("/users/{user}/dependents/{id}/photo",other,id).header("Authorization",managerToken)).andExpect(status().isNotFound());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PATCH,path).file(photo(png(2))).header("Authorization",managerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoUrl").value(path));
        verify(audit).recordInTransaction(argThat(e -> e.action().equals("DEPENDENT_PHOTO_REPLACE")&&e.actorId().equals(manager)));
        mvc.perform(get("/users/{user}/dependents/{id}",owner,id).header("Authorization",managerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.photoUrl").value(path));
        mvc.perform(delete(path).header("Authorization",managerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test void photoOmissionInBothUpdateContractsPreservesThePersistedReference() throws Exception {
        String cpf=cpf();Long id=id(create(ownerToken,cpf,PNG).andExpect(status().isCreated()));String original=key(id);
        mvc.perform(put("/dependents/{id}",id).contentType(MediaType.APPLICATION_JSON).content(data(cpf,"Updated JSON"))
                .header("Authorization",ownerToken)).andExpect(status().isOk());assertThat(key(id)).isEqualTo(original);
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,"/dependents/{id}",id).file(structured(data(cpf,"Updated multipart")))
                .header("Authorization",ownerToken)).andExpect(status().isOk());assertThat(key(id)).isEqualTo(original);
        assertThat(jpa.findById(id).orElseThrow().getName()).isEqualTo("Updated multipart");
        verify(storage,times(1)).store(any(),anyString());verify(storage,never()).delete(anyString());
    }

    @Test void replacementAndIdempotentRemovalNeverDeleteTheOldObjectOrResetOtherFields() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String original=key(id);
        replace(id,ownerToken,png(2)).andExpect(status().isOk());String replacement=key(id);assertThat(replacement).isNotEqualTo(original);
        assertThat(storage.load(original).content()).containsExactly(PNG);assertThat(storage.load(replacement).content()).containsExactly(png(2));
        mvc.perform(delete("/dependents/{id}/photo",id).header("Authorization",ownerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.photoUrl").doesNotExist());
        Instant removed=jpa.findById(id).orElseThrow().getUpdatedAt();assertThat(key(id)).isNull();
        mvc.perform(delete("/dependents/{id}/photo",id).header("Authorization",ownerToken)).andExpect(status().isOk());
        assertThat(jpa.findById(id).orElseThrow().getUpdatedAt()).isEqualTo(removed);assertThat(jpa.findById(id).orElseThrow().getName()).isEqualTo("Private photo dependent");
        verify(storage,never()).delete(anyString());read(id,ownerToken).andExpect(status().isNotFound());
    }

    @Test void multipartUpdateCanReplacePhotoAtomicallyWithTheRegistrationData() throws Exception {
        String cpf=cpf();Long id=id(create(ownerToken,cpf,PNG).andExpect(status().isCreated()));String old=key(id);
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,"/dependents/{id}",id).file(structured(data(cpf,"New registration"))).file(photo(png(3)))
                .header("Authorization",ownerToken)).andExpect(status().isOk());
        assertThat(key(id)).isNotEqualTo(old);assertThat(jpa.findById(id).orElseThrow().getName()).isEqualTo("New registration");
        assertThat(storage.load(old).content()).containsExactly(PNG);read(id,ownerToken).andExpect(content().bytes(png(3)));
    }

    @Test void archivedAndInactivePhotosRemainReadableButMutationIsRejectedBeforeUpload() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String key=key(id);
        dependents.archive(id,owner,owner);read(id,ownerToken).andExpect(status().isOk()).andExpect(content().bytes(PNG));
        replace(id,ownerToken,png(2)).andExpect(status().isConflict());
        mvc.perform(delete("/dependents/{id}/photo",id).header("Authorization",ownerToken)).andExpect(status().isConflict());
        dependents.unarchive(id,owner,owner);read(id,ownerToken).andExpect(status().isOk());replace(id,ownerToken,png(2)).andExpect(status().isConflict());
        assertThat(key(id)).isEqualTo(key);verify(storage,times(1)).store(any(),anyString());
    }

    @Test void deletionSnapshotsTheReferenceAndSharedMediaSurvives() throws Exception {
        Long first=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String shared=key(first);
        Long second=id(create(ownerToken,cpf(),null).andExpect(status().isCreated()));
        tx(() -> {var d=repository.findByIdForUpdate(second).orElseThrow();d.changePhoto(shared,Instant.now(clock));repository.save(d);return null;});
        dependents.delete(first,owner);
        assertThat(history.findAll().stream().filter(h -> h.getDependentId().equals(first))).singleElement().satisfies(h -> assertThat(h.getPhotoStorageKey()).isEqualTo(shared));
        assertThat(jpa.existsById(first)).isFalse();read(first,ownerToken).andExpect(status().isNotFound());read(second,ownerToken).andExpect(status().isOk()).andExpect(content().bytes(PNG));
        assertThat(storage.load(shared).content()).containsExactly(PNG);verify(storage,never()).delete(anyString());
    }

    @Test void invalidFilesDtosPartsAndUntrustedReferencesNeverCreateAnAssociation() throws Exception {
        for(byte[] bytes:List.of(new byte[0],new byte[]{1,2,3},new byte[ImageUpload.MAX_BYTES+1])) {
            create(ownerToken,cpf(),bytes).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/dependents").file(photo(PNG)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/dependents").file(structured(data(cpf(),""))).file(photo(PNG)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        for(String field:List.of("storageKey","photoStorageKey","photo","userId")) {
            String payload=data(cpf(),"Invalid reference").replace("}",",\""+field+"\":\"private-images/forged.png\"}");
            mvc.perform(multipart("/dependents").file(structured(payload)).file(photo(PNG)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/dependents").file(structured(data(cpf(),"Duplicate"))).file(photo(PNG)).file(photo(PNG)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/dependents").file(structured(data(cpf(),"Unexpected"))).param("storageKey","forged").header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        verify(storage,never()).store(any(),anyString());assertThat(jpa.findAllByUserId(owner)).isEmpty();
    }

    @Test void unknownDependentWrongOwnerAndUnauthenticatedRequestsCannotWriteOrReadPhoto() throws Exception {
        Long id=id(create(ownerToken,cpf(),null).andExpect(status().isCreated()));
        replace(id,otherToken,PNG).andExpect(status().isForbidden());read(id,otherToken).andExpect(status().isForbidden());
        replace(Long.MAX_VALUE,ownerToken,PNG).andExpect(status().isNotFound());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PATCH,"/dependents/{id}/photo",id).file(photo(PNG))).andExpect(status().isUnauthorized());
        mvc.perform(get("/dependents/{id}/photo",id)).andExpect(status().isUnauthorized());
        verify(storage,never()).store(any(),anyString());verify(storage,never()).load(anyString());
    }

    @Test void storageFailureIsSanitizedAndCannotPersistAPhotoReference() throws Exception {
        doThrow(new StorageOperationException(StorageOperationException.Operation.WRITE,new IllegalStateException("synthetic private provider details")))
                .when(storage).store(any(),eq("private-images"));
        create(ownerToken,cpf(),PNG).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("IMAGE_STORAGE_UNAVAILABLE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("provider details"))));
        assertThat(jpa.findAllByUserId(owner)).isEmpty();verify(storage,never()).delete(anyString());
    }

    @Test void failedJpaWriteCompensatesOnlyTheFreshPrivateFile() throws Exception {
        long before=files();doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic dependent persistence failure")).when(repository).save(any());
        create(ownerToken,cpf(),PNG).andExpect(status().isInternalServerError());
        assertThat(files()).isEqualTo(before);assertThat(jpa.findAllByUserId(owner)).isEmpty();
        verify(storage).delete(startsWith("private-images/"));
    }

    @Test void failedAuditAfterAssociationRollsBackBothPhotoAndRegistrationAndPreservesThePreviousFile() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String original=key(id);long before=files();
        tx(() -> {doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic photo audit failure")).when(audit).recordInTransaction(argThat(e -> e.action().equals("DEPENDENT_PHOTO_REPLACE")));return null;});
        replace(id,ownerToken,png(2)).andExpect(status().isInternalServerError());
        assertThat(key(id)).isEqualTo(original);assertThat(files()).isEqualTo(before);assertThat(storage.load(original).content()).containsExactly(PNG);
        var deleted=org.mockito.ArgumentCaptor.forClass(String.class);verify(storage).delete(deleted.capture());assertThat(deleted.getValue()).isNotEqualTo(original);
    }

    @Test void failedCompensationKeepsTheOriginalFailureAndTheOrphanIsStillRejectedByTheGlobalReader() throws Exception {
        long before=files();
        doThrow(new IllegalStateException("original synthetic persistence failure")).when(repository).save(any());
        doThrow(new IllegalStateException("private compensation details")).when(storage).delete(anyString());
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(TransactionalImageMediaCommand.class);
        var logs=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();logs.start();logger.addAppender(logs);
        try {create(ownerToken,cpf(),PNG).andExpect(status().isInternalServerError());}
        finally {logger.detachAppender(logs);logs.stop();}
        assertThat(jpa.findAllByUserId(owner)).isEmpty();assertThat(files()).isEqualTo(before+1);
        var orphan=org.mockito.ArgumentCaptor.forClass(String.class);verify(storage).delete(orphan.capture());
        assertThat(logs.list).isNotEmpty().allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(orphan.getValue(),"private compensation details","photo.png",PHOTO_ROOT.toString());
            assertThat(event.getThrowableProxy()).isNull();
        });
        mvc.perform(get("/media/images").param("key",orphan.getValue()).header("Authorization",otherToken)).andExpect(status().isBadRequest());
        assertThat(storage.load(orphan.getValue()).content()).containsExactly(PNG);
    }

    @Test void csvPdfAndPublicContextQueriesDoNotExposePrivateReferences() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String key=key(id);
        assertThat(publicDependents.findDetailsByIds(Set.of(id)).toString()).doesNotContain(key,"private-images");
        assertThat(publicDependents.findDetailsByIds(Set.of(id)).toString()).doesNotContain(key,"private-images");
        for(String format:List.of("CSV","PDF")) {
            var response=mvc.perform(get("/admin/dependents/export").param("format",format).param("userId",owner.toString()).header("Authorization",managerToken))
                    .andExpect(status().isOk()).andReturn().getResponse();
            String text=response.getContentAsString();
            if(format.equals("PDF"))try(var pdf=org.apache.pdfbox.Loader.loadPDF(response.getContentAsByteArray())){text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);}
            assertThat(text).contains("Private photo dependent").doesNotContain(key,"private-images","photoStorageKey");
        }
    }

    @Test void openApiDocumentsBinaryRoutesAndExplicitAdministrativePhotoAuthority() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/dependents/{id}/photo']['get']['responses']['200']['content']['image/*']['schema']['format']").value("binary"))
                .andExpect(jsonPath("$['paths']['/users/{userId}/dependents/{id}/photo']['patch']['x-required-permissions'][0]").value("DEPENDENTS_DEPENDENT_PHOTO_UPDATE"))
                .andExpect(jsonPath("$['paths']['/dependents']['post']['requestBody']['content']['multipart/form-data']").exists())
                .andExpect(jsonPath("$['paths']['/dependents']['post']['requestBody']['content']['application/json']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DependentResponseDTO']['properties']['photoUrl']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DependentResponseDTO']['properties']['photoStorageKey']").doesNotExist());
    }

    @Test void inactiveOwnerCannotMaintainPhotosAndItsOldTokenCannotReadThem() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String original=key(id);
        identities.disable(owner,Instant.now(clock));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PATCH,"/users/{owner}/dependents/{id}/photo",owner,id)
                .file(photo(png(2))).header("Authorization",managerToken)).andExpect(status().isConflict());
        read(id,ownerToken).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/{owner}/dependents/{id}/photo",owner,id).header("Authorization",managerToken))
                .andExpect(status().isOk()).andExpect(content().bytes(PNG));
        assertThat(key(id)).isEqualTo(original);verify(storage,times(1)).store(any(),anyString());
    }

    @Test void photoIsNotReturnedIfItsRequiredTransactionalAuditFails() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String original=key(id);
        tx(() -> {doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic read audit failure")).when(audit).recordInTransaction(argThat(e -> e.action().equals("DEPENDENT_PHOTO_READ")));return null;});
        read(id,ownerToken).andExpect(status().isInternalServerError()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(key(id)).isEqualTo(original);verify(storage,never()).delete(anyString());
    }

    @Test void concurrentPhotoReplacementUsesCurrentStateAndPreservesTheOtherCommittedRegistrationChange() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String initial=key(id);String cpf=jpa.findById(id).orElseThrow().getCpf();
        var executor=Executors.newFixedThreadPool(2);var applied=new CountDownLatch(1);var release=new CountDownLatch(1);var staleRead=new CountDownLatch(1);
        try {
            var first=executor.submit(() -> tx(() -> {
                var result=dependents.update(id,"Winner registration",cpf,LocalDate.of(2015,1,1),RelationshipType.CHILD,null,owner,new ImageUpload("photo.png","image/png",png(2)));
                applied.countDown();await(release);return result;
            }));
            assertThat(applied.await(20,TimeUnit.SECONDS)).isTrue();
            var second=executor.submit(() -> tx(() -> {
                jpa.findById(id).orElseThrow();staleRead.countDown();
                return dependents.replacePhoto(id,owner,new ImageUpload("photo.png","image/png",png(3)));
            }));
            assertThat(staleRead.await(20,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();var earlier=first.get(20,TimeUnit.SECONDS);var later=second.get(20,TimeUnit.SECONDS);
            assertThat(key(id)).isEqualTo(later.photoStorageKey()).isNotEqualTo(earlier.photoStorageKey());
            assertThat(jpa.findById(id).orElseThrow().getName()).isEqualTo("Winner registration");
            assertThat(storage.load(initial).content()).containsExactly(PNG);assertThat(storage.load(earlier.photoStorageKey()).content()).containsExactly(png(2));
            read(id,ownerToken).andExpect(status().isOk()).andExpect(content().bytes(png(3)));verify(storage,never()).delete(anyString());
        } finally {release.countDown();executor.shutdownNow();}
    }

    @Test void deletionWinningAConcurrentReplacementCannotResurrectTheRowOrUploadAnotherFile() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(status().isCreated()));String original=key(id);
        var executor=Executors.newFixedThreadPool(2);var applied=new CountDownLatch(1);var release=new CountDownLatch(1);var staleRead=new CountDownLatch(1);
        try {
            var deletion=executor.submit(() -> tx(() -> {dependents.delete(id,owner);applied.countDown();await(release);return true;}));
            assertThat(applied.await(20,TimeUnit.SECONDS)).isTrue();
            var replacement=executor.submit(() -> {
                try {return tx(() -> {jpa.findById(id).orElseThrow();staleRead.countDown();dependents.replacePhoto(id,owner,new ImageUpload("photo.png","image/png",png(2)));return "UNEXPECTED_SUCCESS";});}
                catch(com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException expected){return "NOT_FOUND";}
            });
            assertThat(staleRead.await(20,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> replacement.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();assertThat(deletion.get(20,TimeUnit.SECONDS)).isTrue();assertThat(replacement.get(20,TimeUnit.SECONDS)).isEqualTo("NOT_FOUND");
            assertThat(jpa.existsById(id)).isFalse();assertThat(history.findAll().stream().filter(h -> h.getDependentId().equals(id))).singleElement().satisfies(h -> assertThat(h.getPhotoStorageKey()).isEqualTo(original));
            verify(storage,times(1)).store(any(),anyString());verify(storage,never()).delete(anyString());
        } finally {release.countDown();executor.shutdownNow();}
    }

    static void await(CountDownLatch latch){try{if(!latch.await(20,TimeUnit.SECONDS))throw new IllegalStateException("Synthetic photo race fixture timeout");}catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException(failure);}}

    Long user() {return tx(() -> {var now=Instant.now(clock);var u=users.create(User.create("Synthetic photo actor",null,null,cpf(),null,null,null,now));accounts.create(AuthenticationAccount.create(u.getId(),"synthetic-unusable-password-hash",now));return u.getId();});}
    void grant(Long user,PermissionCode... codes) {tx(() -> {var now=Instant.now(clock);var role=roles.save(Role.create("Photo authority "+UUID.randomUUID(),null,now));for(var code:codes){var permission=permissions.findByCode(code).orElseGet(() -> permissions.save(Permission.fromDefinition(PermissionDefinition.from(code),now)));rolePermissions.save(RolePermission.create(role.getId(),permission.getId(),now));}userRoles.save(UserRole.create(user,role.getId(),now));return null;});}
    String token(Long user) {var session=tx(() -> sessions.save(Session.create(user,Duration.ofHours(1),Instant.now(clock))));String token="photo-test-"+UUID.randomUUID();when(parser.parseAndValidate(token)).thenReturn(new JwtAuthenticatedUser(user,session.getId(),"Synthetic photo actor",Instant.now(clock).plusSeconds(3600)));return "Bearer "+token;}
    <T>T tx(Supplier<T> work){return transactions.execute(status -> work.get());}
    String key(Long id){return jpa.findById(id).orElseThrow().getPhotoStorageKey();}
    Long id(ResultActions response)throws Exception{return ((Number)JsonPath.read(response.andReturn().getResponse().getContentAsString(),"$.id")).longValue();}
    ResultActions create(String token,String cpf,byte[] image)throws Exception {var request=multipart("/dependents").file(structured(data(cpf,"Private photo dependent"))).header("Authorization",token);if(image!=null)request.file(photo(image));return mvc.perform(request);}
    ResultActions replace(Long id,String token,byte[] bytes)throws Exception{return mvc.perform(multipart(org.springframework.http.HttpMethod.PATCH,"/dependents/{id}/photo",id).file(photo(bytes)).header("Authorization",token));}
    ResultActions read(Long id,String token)throws Exception{return mvc.perform(get("/dependents/{id}/photo",id).header("Authorization",token));}
    MockMultipartFile structured(String json){return new MockMultipartFile("dependent","","application/json",json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    MockMultipartFile photo(byte[] bytes){return new MockMultipartFile("photo","photo.png","image/png",bytes);}
    String data(String cpf,String name){return "{\"name\":\""+name+"\",\"cpf\":\""+cpf+"\",\"birthDate\":\"2015-01-01\",\"relationshipType\":\"CHILD\",\"phoneNumber\":null}";}
    byte[] png(int marker){var bytes=PNG.clone();bytes[bytes.length-1]=(byte)marker;return bytes;}
    long files()throws Exception{try(var files=Files.walk(PHOTO_ROOT)){return files.filter(Files::isRegularFile).count();}}
    static String cpf(){String value=Integer.toString(CPFS.incrementAndGet());for(int length=9;length<11;length++){int sum=0;for(int i=0;i<length;i++)sum+=(value.charAt(i)-'0')*(length+1-i);int mod=sum%11;value+=(mod<2?0:11-mod);}return value;}
}
