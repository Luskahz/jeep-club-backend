package com.jeepclub.backend.vehicles.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.jeepclub.backend.vehicles.core.application.VehicleGalleryChange;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.*;
import com.jeepclub.backend.vehicles.core.domain.enums.*;
import com.jeepclub.backend.vehicles.core.domain.model.*;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.*;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.*;
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

/** Committed HTTP transactions, real local storage and actual Identity/Authorization. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class VehicleGalleryIntegrationTest {
    @TempDir static Path PHOTO_ROOT;
    @DynamicPropertySource static void storageRoot(DynamicPropertyRegistry r) {r.add("storage.local.root-directory",()->PHOTO_ROOT.toString());}
    static final byte[] PNG={(byte)137,80,78,71,13,10,26,10,1};
    static final AtomicInteger NUMBERS=new AtomicInteger(850000000);
    @Autowired MockMvc mvc;
    @Autowired VehicleService vehicles;
    @Autowired AdminVehicleService admin;
    @Autowired VehicleJpaRepository jpa;
    @Autowired VehicleHistoryJpaRepository histories;
    @Autowired UserRepository users;
    @Autowired AuthenticationAccountRepository accounts;
    @Autowired SessionRepository sessions;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired UserRoleRepository userRoles;
    @Autowired TransactionTemplate transactions;
    @Autowired Clock clock;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired jakarta.persistence.EntityManagerFactory emf;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.jeepclub.backend.vehicles.api.module.EventVehicleQuery publicVehicles;
    @Autowired com.jeepclub.backend.iam.identity.api.module.UserAdministration identities;
    @MockitoBean JwtTokenParser parser;
    @MockitoSpyBean FileStorage storage;
    @MockitoSpyBean VehicleRepository repository;
    @MockitoSpyBean SystemLogService audit;
    Long owner,other,manager,reader;
    String ownerToken,otherToken,managerToken,readerToken;

    @BeforeEach void actors() {
        owner=user();other=user();manager=user();reader=user();
        grant(manager,PermissionCode.VEHICLES_VEHICLE_CREATE,PermissionCode.VEHICLES_VEHICLE_READ,PermissionCode.VEHICLES_VEHICLE_UPDATE,PermissionCode.VEHICLES_VEHICLE_DELETE,PermissionCode.VEHICLES_VEHICLE_EXPORT);
        grant(reader,PermissionCode.VEHICLES_VEHICLE_READ);
        ownerToken=token(owner);otherToken=token(other);managerToken=token(manager);readerToken=token(reader);
    }

    @ParameterizedTest @ValueSource(ints={0,1,5}) void createsAndReadsTheCompleteGallery(int count)throws Exception {
        Long id=create(count);var v=vehicles.findById(id,owner);assertThat(v.getImages()).hasSize(count);assertThat(v.getGalleryRevision()).isZero();
        var detail=detail(id,ownerToken).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(count)).andExpect(jsonPath("$.galleryRevision").value(0));
        if(count>0) {
            detail.andExpect(jsonPath("$.photo").value(v.getPhoto())).andExpect(jsonPath("$.images[0].primary").value(true));
            for(var image:v.getImages()) assertThat(storage.load(image.storageKey()).content()).startsWith(Arrays.copyOf(PNG,8));
            mvc.perform(get("/media/images").param("key",v.getPhoto()).header("Authorization",otherToken)).andExpect(status().isOk());
        } else {detail.andExpect(jsonPath("$.photo").doesNotExist());verify(storage,never()).store(any(),anyString());}
        mvc.perform(get("/vehicles/detail-for-edit/member/{id}",id).header("Authorization",ownerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(count));
        mvc.perform(get("/vehicles/detail/admin/{id}",id).header("Authorization",managerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(count));
        mvc.perform(get("/vehicles/detail-for-edit/admin/{id}",id).header("Authorization",managerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(count));
        verify(storage,never()).delete(anyString());
    }

    @Test void createsWithExplicitOrderAndPrimaryAndAdminUsesTheChosenOwner()throws Exception {
        String gallery="{\"images\":[{\"uploadIndex\":0,\"position\":1,\"primary\":true},{\"uploadIndex\":1,\"position\":0,\"primary\":false}]}";
        var request=multipart("/vehicles/include/admin/{owner}",owner).file(part("vehicle",data())).file(part("gallery",gallery)).file(photo(1)).file(photo(2));
        mvc.perform(request.header("Authorization",readerToken)).andExpect(status().isForbidden());verify(storage,never()).store(any(),anyString());
        mvc.perform(multipart("/vehicles/include/admin/{owner}",owner).file(part("vehicle",data())).file(part("gallery",gallery)).file(photo(1)).file(photo(2)).header("Authorization",managerToken)).andExpect(status().isCreated());
        var v=vehicles.findAll(owner,org.springframework.data.domain.PageRequest.of(0,10)).getContent().get(0);
        assertThat(v.getOwnerId()).isEqualTo(owner);assertThat(v.getImages()).extracting(VehicleImage::position).containsExactly(0,1);
        assertThat(storage.load(v.getImages().get(0).storageKey()).content()[8]).isEqualTo((byte)2);
        assertThat(v.getPhoto()).isEqualTo(v.getImages().get(1).storageKey());
        verify(audit).recordInTransaction(argThat(e->e.action().equals("VEHICLE_GALLERY_CREATE")&&e.actorId().equals(manager)));
    }

    @Test void rejectsSixFilesUnknownFieldsAndAmbiguousLegacyPhotoBeforeStorage()throws Exception {
        var six=multipart("/vehicles/include/member").file(part("vehicle",data())).header("Authorization",ownerToken);
        for(int i=0;i<6;i++)six.file(photo(i));mvc.perform(six).andExpect(status().isBadRequest());
        mvc.perform(multipart("/vehicles/include/member").file(part("vehicle",data())).file(new MockMultipartFile("photo","old.png","image/png",PNG)).file(photo(1)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/vehicles/include/member").file(part("vehicle",data().replace("\"nickname\"","\"storageKey\""))).file(photo(1)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        assertThat(vehicles.findAll(owner,org.springframework.data.domain.PageRequest.of(0,10))).isEmpty();verify(storage,never()).store(any(),anyString());
    }

    @Test void invalidCompleteMetadataNeverProducesPartialImages()throws Exception {
        long before=files();
        String[] invalid={"null","[]","{}","{\"images\":null}","{\"images\":[null]}",
            "{\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":false}]}",
            "{\"images\":[{\"uploadIndex\":0,\"position\":1,\"primary\":true}]}",
            "{\"images\":[{\"uploadIndex\":1,\"position\":0,\"primary\":true}]}",
            "{\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true},{\"uploadIndex\":0,\"position\":1,\"primary\":false}]}",
            "{\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true,\"storageKey\":\"forged\"}]}",
            "{\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true,\"providerPath\":\"forged\"}]}"};
        for(var json:invalid) mvc.perform(multipart("/vehicles/include/member").file(part("vehicle",data())).file(part("gallery",json)).file(photo(1)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        verify(storage,never()).store(any(),anyString());assertThat(files()).isEqualTo(before);
    }

    @Test void reordersAndChangesPrimaryWithoutUploadAndExactRetryIsStable()throws Exception {
        Long id=create(5);var initial=vehicles.findById(id,owner);var reversed=new ArrayList<VehicleImage>();
        for(int i=0;i<5;i++)reversed.add(new VehicleImage(initial.getImages().get(4-i).storageKey(),i,i==3));
        var request=payload(0,reversed);replace(id,ownerToken,request).andExpect(status().isOk()).andExpect(jsonPath("$.galleryRevision").value(1));
        var changed=vehicles.findById(id,owner);assertThat(changed.getImages()).isEqualTo(reversed);assertThat(changed.getPhoto()).isEqualTo(reversed.get(3).storageKey());
        replace(id,ownerToken,request).andExpect(status().isOk()).andExpect(jsonPath("$.galleryRevision").value(1));
        assertThat(vehicles.findById(id,owner).getUpdatedAt()).isEqualTo(changed.getUpdatedAt());verify(storage,times(5)).store(any(),anyString());verify(storage,never()).delete(anyString());
        mvc.perform(get("/vehicles/list/member").header("Authorization",ownerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].photo").value(changed.getPhoto()));
    }

    @Test void removingPrimaryRequiresAnotherPrimaryAndExplicitEmptyClearsTheGallery()throws Exception {
        long filesBefore=files();
        Long id=create(2);var initial=vehicles.findById(id,owner);String retained=initial.getImages().get(1).storageKey();
        replace(id,ownerToken,payload(0,List.of(new VehicleImage(retained,0,false)))).andExpect(status().isBadRequest());
        replace(id,ownerToken,payload(0,List.of(new VehicleImage(retained,0,true)))).andExpect(status().isOk()).andExpect(jsonPath("$.photo").value(retained));
        replace(id,ownerToken,"{\"expectedRevision\":1,\"images\":[]}").andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(0)).andExpect(jsonPath("$.photo").doesNotExist());
        assertThat(jpa.findById(id).orElseThrow().getPhoto()).isNull();assertThat(files()).isEqualTo(filesBefore+2);verify(storage,never()).delete(anyString());
    }

    @Test void partialRegistrationEditPreservesGalleryAndLegacyAliasCannotEraseSeveralImages()throws Exception {
        Long id=create(5);var initial=vehicles.findById(id,owner);
        mvc.perform(put("/vehicles/edit/member/{id}",id).contentType("application/json").content("{\"nickname\":\"Edited registration\"}").header("Authorization",ownerToken)).andExpect(status().isNoContent());
        assertThat(vehicles.findById(id,owner).getImages()).isEqualTo(initial.getImages());
        mvc.perform(put("/vehicles/edit/member/{id}",id).contentType("application/json").content("{\"photo\":null}").header("Authorization",ownerToken)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VEHICLE_GALLERY_CONFLICT"));
        mvc.perform(put("/vehicles/edit/member/{id}",id).contentType("application/json").content("{\"images\":[]}").header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        assertThat(vehicles.findById(id,owner).getGalleryRevision()).isZero();verify(storage,never()).delete(anyString());
    }

    @Test void retainedLegacyUrlSurvivesUnrelatedEditAndConsciousReplacementDoesNotDeleteIt()throws Exception {
        String legacy="https://legacy.invalid/Photo.PNG?old=1";Long id=legacy(legacy);var before=vehicles.findById(id,owner);
        mvc.perform(put("/vehicles/edit/member/{id}",id).contentType("application/json").content("{\"nickname\":\"Legacy preserved\",\"photo\":\""+legacy+"\"}").header("Authorization",ownerToken)).andExpect(status().isNoContent());
        replace(id,ownerToken,payload(0,before.getImages())).andExpect(status().isOk()).andExpect(jsonPath("$.photo").value(legacy));
        var second="{\"expectedRevision\":0,\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true}]}";
        uploadReplace(id,ownerToken,second,1).andExpect(status().isOk()).andExpect(jsonPath("$.galleryRevision").value(1));
        verify(storage,never()).load(legacy);verify(storage,never()).delete(anyString());
    }

    @Test void rejectsInvalidMissingAndPrivateNewReferencesAndDuplicateKeys()throws Exception {
        Long id=create(1);var old=vehicles.findById(id,owner);String key=old.getPhoto();
        for(String bad:List.of("../unsafe","https://legacy.invalid/forged.png","private-images/2026/10/19/00000000-0000-0000-0000-000000000001.png"))
            replace(id,ownerToken,payload(0,List.of(new VehicleImage(bad,0,true)))).andExpect(status().isBadRequest());
        replace(id,ownerToken,payload(0,List.of(new VehicleImage("images/2026/10/19/00000000-0000-0000-0000-999999999999.png",0,true)))).andExpect(status().isNotFound());
        replace(id,ownerToken,payload(0,List.of(new VehicleImage(key,0,true),new VehicleImage(key,1,false)))).andExpect(status().isBadRequest());
        replace(id,ownerToken,"{\"images\":[]}").andExpect(status().isBadRequest());assertThat(vehicles.findById(id,owner).getGallery()).isEqualTo(old.getGallery());
    }

    @Test void ownershipAuthoritiesAndLifecycleApplyToBothSurfaces()throws Exception {
        Long id=create(1);String empty="{\"expectedRevision\":0,\"images\":[]}";
        mvc.perform(put("/vehicles/gallery/member/{id}",id).contentType("application/json").content(empty)).andExpect(status().isUnauthorized());
        replace(id,otherToken,empty).andExpect(status().isNotFound());detail(id,otherToken).andExpect(status().isNotFound());
        mvc.perform(put("/vehicles/gallery/admin/{id}",id).contentType("application/json").content(empty).header("Authorization",readerToken)).andExpect(status().isForbidden());
        vehicles.archive(id,owner);replace(id,ownerToken,empty).andExpect(status().isConflict());
        mvc.perform(put("/vehicles/gallery/admin/{id}",id).contentType("application/json").content(empty).header("Authorization",managerToken)).andExpect(status().isConflict());
        detail(id,ownerToken).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(1));
        vehicles.unarchive(id,owner);replace(id,ownerToken,empty).andExpect(status().isConflict());vehicles.activate(id,owner);
        mvc.perform(put("/vehicles/gallery/admin/{id}",id).contentType("application/json").content(empty).header("Authorization",managerToken)).andExpect(status().isOk());
        verify(audit).recordInTransaction(argThat(e->e.action().equals("VEHICLE_GALLERY_REPLACE")&&e.actorId().equals(manager)));
    }

    @Test void inactiveOwnerCannotUploadAndOldAccessIsRevoked()throws Exception {
        Long id=create(1);identities.archive(owner,manager,Instant.now(clock));
        detail(id,ownerToken).andExpect(status().isUnauthorized());
        uploadReplace(id,managerToken,"{\"expectedRevision\":0,\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true}]}",1,true).andExpect(status().isConflict());
        assertThat(vehicles.findById(id,owner).getImages()).hasSize(1);verify(storage,times(1)).store(any(),anyString());
    }

    @Test void deletedGalleryIsIndependentReadableWithHistoryPermissionAndFilesAreRetained()throws Exception {
        Long id=create(5);var before=vehicles.findById(id,owner);vehicles.delete(id,owner);
        assertThat(jpa.existsById(id)).isFalse();Long history=histories.findAll().stream().filter(h->h.getVehicleId().equals(id)).findFirst().orElseThrow().getId();
        mvc.perform(get("/vehicles/gallery/history/admin/{id}",history).header("Authorization",readerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/vehicles/gallery/history/admin/{id}",history).header("Authorization",managerToken)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.vehicleId").value(id)).andExpect(jsonPath("$.images.length()").value(5));
        assertThat(admin.galleryHistory(history,manager).gallery().images()).isEqualTo(before.getImages());
        detail(id,ownerToken).andExpect(status().isNotFound());for(var image:before.getImages())assertThat(storage.load(image.storageKey()).content()).isNotEmpty();verify(storage,never()).delete(anyString());
    }

    @Test void failureOnThirdUploadCompensatesOnlyFilesCreatedByThisRequest()throws Exception {
        Long id=create(1);var before=vehicles.findById(id,owner);long filesBefore=files();var writes=new AtomicInteger();
        doAnswer(a->{if(writes.incrementAndGet()==3)throw new StorageOperationException(StorageOperationException.Operation.WRITE,new java.io.IOException("synthetic-private-provider-path"));return a.callRealMethod();}).when(storage).store(any(),eq("images"));
        var request=multipart("/vehicles/include/member").file(part("vehicle",data())).header("Authorization",ownerToken);for(int i=0;i<5;i++)request.file(photo(i));
        mvc.perform(request).andExpect(status().isServiceUnavailable()).andExpect(r->assertThat(r.getResponse().getContentAsString()).doesNotContain("synthetic-private-provider-path"));
        assertThat(files()).isEqualTo(filesBefore);assertThat(vehicles.findById(id,owner).getGallery()).isEqualTo(before.getGallery());verify(storage,times(2)).delete(argThat(key->!key.equals(before.getPhoto())));
    }

    @Test void persistenceFailureRollsBackGalleryAndCompensatesAllFiveNewFiles()throws Exception {
        Long id=create(1);var before=vehicles.findById(id,owner);long filesBefore=files();
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic gallery persistence failure")).when(repository).save(argThat(v->v!=null&&v.getId()!=null&&v.getId().equals(id)));
        String request="{\"expectedRevision\":0,\"images\":["+java.util.stream.IntStream.range(0,5).mapToObj(i->"{\"uploadIndex\":"+i+",\"position\":"+i+",\"primary\":"+(i==0)+"}").collect(java.util.stream.Collectors.joining(","))+"]}";
        uploadReplace(id,ownerToken,request,5).andExpect(status().isInternalServerError());
        assertThat(files()).isEqualTo(filesBefore);assertThat(vehicles.findById(id,owner).getGallery()).isEqualTo(before.getGallery());assertThat(vehicles.findById(id,owner).getGalleryRevision()).isZero();verify(storage,times(5)).delete(argThat(key->!key.equals(before.getPhoto())));
    }

    @Test void auditFailureAfterJpaFlushRollsBackBothGalleryAndNewImage()throws Exception {
        Long id=create(2);var before=vehicles.findById(id,owner);long filesBefore=files();
        tx(()->{doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic gallery audit failure")).when(audit).recordInTransaction(argThat(e->e.action().equals("VEHICLE_GALLERY_REPLACE")));return null;});
        uploadReplace(id,ownerToken,"{\"expectedRevision\":0,\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true}]}",1).andExpect(status().isInternalServerError());
        assertThat(vehicles.findById(id,owner).getGallery()).isEqualTo(before.getGallery());assertThat(files()).isEqualTo(filesBefore);verify(storage,times(1)).delete(argThat(key->before.getImages().stream().noneMatch(i->i.storageKey().equals(key))));
    }

    @Test void failingCompensationPreservesOriginalErrorAndNeverRemovesAnExistingImage()throws Exception {
        Long id=create(1);var before=vehicles.findById(id,owner);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("original-gallery-error")).when(repository).save(argThat(v->v!=null&&id.equals(v.getId())));
        doThrow(new StorageOperationException(StorageOperationException.Operation.DELETE,new java.io.IOException("synthetic cleanup diagnostic"))).when(storage).delete(anyString());
        assertThatThrownBy(()->vehicles.replaceGallery(id,owner,VehicleGalleryChange.uploads(List.of(new ImageUpload("new.png","image/png",PNG))))).isInstanceOf(IllegalArgumentException.class);
        var change=new VehicleGalleryChange(0L,List.of(new VehicleGalleryChange.Entry(null,0,0,true)),List.of(new ImageUpload("new.png","image/png",PNG)));
        assertThatThrownBy(()->vehicles.replaceGallery(id,owner,change)).isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class).hasMessage("original-gallery-error");
        assertThat(vehicles.findById(id,owner).getGallery()).isEqualTo(before.getGallery());assertThat(storage.load(before.getPhoto()).content()).isNotEmpty();verify(storage).delete(argThat(key->!key.equals(before.getPhoto())));
    }

    @Test void publicSharedImageCanBeRetainedWithoutDeletingEitherAssociationOrHistory()throws Exception {
        Long first=create(1);String shared=vehicles.findById(first,owner).getPhoto();Long second=create(0);
        replace(second,ownerToken,payload(0,List.of(new VehicleImage(shared,0,true)))).andExpect(status().isOk());
        replace(first,ownerToken,"{\"expectedRevision\":0,\"images\":[]}").andExpect(status().isOk());vehicles.delete(first,owner);
        assertThat(vehicles.findById(second,owner).getPhoto()).isEqualTo(shared);assertThat(storage.load(shared).content()).isNotEmpty();verify(storage,never()).delete(anyString());
        vehicles.delete(second,owner);Long h=histories.findAll().stream().filter(x->x.getVehicleId().equals(second)).findFirst().orElseThrow().getId();assertThat(admin.galleryHistory(h,manager).gallery().primaryReference()).isEqualTo(shared);assertThat(storage.load(shared).content()).isNotEmpty();
    }

    @Test void invalidFilesAndUnreferencedUploadsNeverChangeTheGallery()throws Exception {
        Long id=create(1);var before=vehicles.findById(id,owner);long beforeFiles=files();
        String gallery="{\"expectedRevision\":0,\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true}]}";
        for(var file:List.of(new MockMultipartFile("photos","bad.png","image/png",new byte[0]),
                new MockMultipartFile("photos","bad.png","image/png",new byte[]{1,2,3}),
                new MockMultipartFile("photos","bad.png","text/plain",PNG),
                new MockMultipartFile("photos","bad.png","image/png",new byte[ImageUpload.MAX_BYTES+1])))
            mvc.perform(multipart(HttpMethod.PUT,"/vehicles/gallery/member/{id}",id).file(part("gallery",gallery)).file(file).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        uploadReplace(id,ownerToken,"{\"expectedRevision\":0,\"images\":[]}",1).andExpect(status().isBadRequest());
        mvc.perform(multipart(HttpMethod.PUT,"/vehicles/gallery/member/{id}",id).file(part("gallery",gallery)).file(photo(1)).file(new MockMultipartFile("provider","x","text/plain",PNG)).header("Authorization",ownerToken)).andExpect(status().isBadRequest());
        assertThat(vehicles.findById(id,owner).getGallery()).isEqualTo(before.getGallery());assertThat(files()).isEqualTo(beforeFiles);verify(storage,times(1)).store(any(),anyString());
    }

    @Test void pageFetchesImagesInOneBatchWithoutDuplicatingVehicles()throws Exception {
        var expected=new ArrayList<Long>();for(int i=0;i<5;i++)expected.add(create(5));
        var statistics=emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();statistics.setStatisticsEnabled(true);statistics.clear();
        try {
            var page=vehicles.findAll(owner,org.springframework.data.domain.PageRequest.of(0,2,org.springframework.data.domain.Sort.by("id")));
            assertThat(page.getTotalElements()).isEqualTo(5);assertThat(page.getContent()).extracting(Vehicle::getId).containsExactlyElementsOf(expected.subList(0,2));
            assertThat(page.getContent()).allSatisfy(v->assertThat(v.getImages()).hasSize(5));
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);assertThat(statistics.getCollectionFetchCount()).isZero();
        } finally {statistics.setStatisticsEnabled(false);}
        mvc.perform(get("/vehicles/list/member").param("size","2").param("page","1").header("Authorization",ownerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5)).andExpect(jsonPath("$.content.length()").value(2));
        long active=jpa.findAll().stream().filter(v->v.getStatus()==VehicleStatus.ACTIVE&&!v.isLegacyDeleted()).count();
        mvc.perform(get("/vehicles/list/admin").param("size","2").header("Authorization",managerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(active)).andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test void exportsUseTheCollectionEvenIfLegacyProjectionIsNullAndDoNotExposeKeys()throws Exception {
        Long id=create(5);var v=vehicles.findById(id,owner);jdbc.update("update vehicles_vehicle set photo=null where id=?",id);
        var csv=mvc.perform(get("/vehicles/admin/export").param("id",id.toString()).param("format","CSV").header("Authorization",managerToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains(v.getPlate(),"Sim").doesNotContain("images/","private-images/",PHOTO_ROOT.toString());
        var pdf=mvc.perform(get("/vehicles/admin/export").param("id",id.toString()).param("format","PDF").header("Authorization",managerToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var document=org.apache.pdfbox.Loader.loadPDF(pdf)){assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(document)).contains(v.getPlate(),"Sim").doesNotContain("images/","private-images/");}
        assertThat(publicVehicles.findDetailsBatch(List.of(id)).toString()).doesNotContain("images/","private-images/");
        vehicles.delete(id,owner);Long history=histories.findAll().stream().filter(h->h.getVehicleId().equals(id)).findFirst().orElseThrow().getId();jdbc.update("update vehicles_vehicle_history set photo=null where id=?",history);
        String historyCsv=mvc.perform(get("/vehicles/admin/history/export").header("Authorization",managerToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(historyCsv).contains(v.getPlate(),"Sim").doesNotContain("images/","private-images/");
        var historyPdf=mvc.perform(get("/vehicles/admin/history/export").param("format","PDF").header("Authorization",managerToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var document=org.apache.pdfbox.Loader.loadPDF(historyPdf)){assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(document)).contains(v.getPlate(),"Sim").doesNotContain("images/","private-images/");}
    }

    @Test void staleConcurrentReplacementIsRejectedUsingCurrentCollectionAndCannotLoseImages()throws Exception {
        Long id=create(5);var before=vehicles.findById(id,owner);var entries=new ArrayList<VehicleGalleryChange.Entry>();entries.add(new VehicleGalleryChange.Entry(null,0,0,true));
        for(int i=1;i<5;i++)entries.add(new VehicleGalleryChange.Entry(before.getImages().get(i).storageKey(),null,i,false));
        var change=new VehicleGalleryChange(0L,entries,List.of(new ImageUpload("winner.png","image/png",PNG)));
        var executor=Executors.newFixedThreadPool(2);var applied=new CountDownLatch(1);var release=new CountDownLatch(1);var staleRead=new CountDownLatch(1);
        try {
            var winner=executor.submit(()->tx(()->{var result=vehicles.replaceGallery(id,owner,change);applied.countDown();await(release);return result;}));
            assertThat(applied.await(20,TimeUnit.SECONDS)).isTrue();
            var loser=executor.submit(()->{try{return tx(()->{repository.findById(id).orElseThrow();staleRead.countDown();vehicles.replaceGallery(id,owner,retained(0,before.getImages()));return "UNEXPECTED_SUCCESS";});}catch(com.jeepclub.backend.vehicles.core.domain.exception.VehicleGalleryConflictException expected){return "CONFLICT";}});
            assertThat(staleRead.await(20,TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->loser.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();var result=winner.get(20,TimeUnit.SECONDS);assertThat(loser.get(20,TimeUnit.SECONDS)).isEqualTo("CONFLICT");
            assertThat(vehicles.findById(id,owner).getGallery()).isEqualTo(result.getGallery());assertThat(result.getImages()).hasSize(5);assertThat(result.getGalleryRevision()).isEqualTo(1);assertThat(result.getPhoto()).isNotEqualTo(before.getPhoto());verify(storage,times(6)).store(any(),anyString());verify(storage,never()).delete(anyString());
        } finally {release.countDown();executor.shutdownNow();}
    }

    @Test void staleRevisionWithNewFileIsRejectedBeforeUploadingIt()throws Exception {
        Long id=create(1);vehicles.replaceGallery(id,owner,new VehicleGalleryChange(0L,List.of(),List.of()));
        uploadReplace(id,ownerToken,"{\"expectedRevision\":0,\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true}]}",1).andExpect(status().isConflict());verify(storage,times(1)).store(any(),anyString());assertThat(vehicles.findById(id,owner).getImages()).isEmpty();
    }

    @Test void deletionWinnerPreventsAStaleGalleryReplacementFromRecreatingTheVehicle()throws Exception {
        Long id=create(5);var before=vehicles.findById(id,owner);var executor=Executors.newFixedThreadPool(2);var applied=new CountDownLatch(1);var release=new CountDownLatch(1);var staleRead=new CountDownLatch(1);
        try {
            var winner=executor.submit(()->tx(()->{vehicles.delete(id,owner);applied.countDown();await(release);return true;}));assertThat(applied.await(20,TimeUnit.SECONDS)).isTrue();
            var loser=executor.submit(()->{try{return tx(()->{repository.findById(id).orElseThrow();staleRead.countDown();vehicles.replaceGallery(id,owner,new VehicleGalleryChange(0L,List.of(new VehicleGalleryChange.Entry(null,0,0,true)),List.of(new ImageUpload("loser.png","image/png",PNG))));return "UNEXPECTED_SUCCESS";});}catch(com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException expected){return "NOT_FOUND";}});
            assertThat(staleRead.await(20,TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->loser.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);release.countDown();assertThat(winner.get(20,TimeUnit.SECONDS)).isTrue();assertThat(loser.get(20,TimeUnit.SECONDS)).isEqualTo("NOT_FOUND");
            assertThat(jpa.existsById(id)).isFalse();Long h=histories.findAll().stream().filter(x->x.getVehicleId().equals(id)).findFirst().orElseThrow().getId();assertThat(admin.galleryHistory(h,manager).gallery()).isEqualTo(before.getGallery());verify(storage,times(5)).store(any(),anyString());verify(storage,never()).delete(anyString());
        } finally {release.countDown();executor.shutdownNow();}
    }

    @Test void deletionAfterConcurrentGalleryUpdateSnapshotsTheWinnerInsteadOfItsOldMvccCollection()throws Exception {
        Long id=create(2);var before=vehicles.findById(id,owner);var executor=Executors.newFixedThreadPool(2);var applied=new CountDownLatch(1);var release=new CountDownLatch(1);var staleRead=new CountDownLatch(1);
        var change=new VehicleGalleryChange(0L,List.of(new VehicleGalleryChange.Entry(null,0,0,true)),List.of(new ImageUpload("winner.png","image/png",PNG)));
        try {
            var winner=executor.submit(()->tx(()->{var result=vehicles.replaceGallery(id,owner,change);applied.countDown();await(release);return result;}));assertThat(applied.await(20,TimeUnit.SECONDS)).isTrue();
            var deletion=executor.submit(()->tx(()->{repository.findById(id).orElseThrow();staleRead.countDown();vehicles.delete(id,owner);return true;}));
            assertThat(staleRead.await(20,TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->deletion.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);release.countDown();var result=winner.get(20,TimeUnit.SECONDS);assertThat(deletion.get(20,TimeUnit.SECONDS)).isTrue();
            Long h=histories.findAll().stream().filter(x->x.getVehicleId().equals(id)).findFirst().orElseThrow().getId();var snapshot=admin.galleryHistory(h,manager);assertThat(snapshot.gallery()).isEqualTo(result.getGallery());assertThat(snapshot.galleryRevision()).isEqualTo(1);assertThat(snapshot.gallery()).isNotEqualTo(before.getGallery());assertThat(jpa.existsById(id)).isFalse();verify(storage,never()).delete(anyString());
        } finally {release.countDown();executor.shutdownNow();}
    }

    @Test void staleLegacyPhotoWriterCannotBypassVersionedGalleryConcurrencyProtection()throws Exception {
        Long id=create(1);var before=vehicles.findById(id,owner);var executor=Executors.newFixedThreadPool(2);var applied=new CountDownLatch(1);var release=new CountDownLatch(1);var staleRead=new CountDownLatch(1);
        try {
            var winner=executor.submit(()->tx(()->{vehicles.replaceGallery(id,owner,new VehicleGalleryChange(0L,List.of(),List.of()));applied.countDown();await(release);return true;}));assertThat(applied.await(20,TimeUnit.SECONDS)).isTrue();
            var loser=executor.submit(()->{try{return tx(()->{repository.findById(id).orElseThrow();staleRead.countDown();vehicles.update(id,owner,legacyPhoto(before.getPhoto()));return "UNEXPECTED_SUCCESS";});}catch(com.jeepclub.backend.vehicles.core.domain.exception.VehicleGalleryConflictException expected){return "CONFLICT";}});
            assertThat(staleRead.await(20,TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->loser.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);release.countDown();assertThat(winner.get(20,TimeUnit.SECONDS)).isTrue();assertThat(loser.get(20,TimeUnit.SECONDS)).isEqualTo("CONFLICT");
            assertThat(vehicles.findById(id,owner).getImages()).isEmpty();assertThat(vehicles.findById(id,owner).getGalleryRevision()).isEqualTo(1);verify(storage,never()).load(anyString());verify(storage,never()).delete(anyString());
        } finally {release.countDown();executor.shutdownNow();}
    }

    com.jeepclub.backend.vehicles.core.application.VehicleEditFields legacyPhoto(String key) {
        return new com.jeepclub.backend.vehicles.core.application.VehicleEditFields(
                com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),com.jeepclub.backend.vehicles.core.application.FieldUpdate.of(key),
                com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),
                com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),
                com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),
                com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),
                com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted(),
                com.jeepclub.backend.vehicles.core.application.FieldUpdate.omitted());
    }

    @Test void openApiPublishesMultipartAndJsonGalleryRevisionAndHistoryAuthorities()throws Exception {
        String schema=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat((Object)JsonPath.read(schema,"$.paths['/vehicles/gallery/member/{vehicleId}'].put.requestBody.content")).isNotNull();
        assertThat((Map<String,Object>)JsonPath.read(schema,"$.paths['/vehicles/gallery/member/{vehicleId}'].put.requestBody.content")).containsKeys("application/json","multipart/form-data");
        assertThat((Object)JsonPath.read(schema,"$.components.schemas.DetailResponseDTO.properties.images")).isNotNull();
        assertThat((Object)JsonPath.read(schema,"$.components.schemas.DetailResponseDTO.properties.galleryRevision")).isNotNull();
        assertThat((List<String>)JsonPath.read(schema,"$.components.schemas.VehicleGalleryUpdateRequest.required")).contains("expectedRevision","images");
        assertThat(schema).contains("/vehicles/gallery/history/admin/{historyId}","VEHICLES_VEHICLE_EXPORT","VEHICLES_VEHICLE_UPDATE","uploadIndex");
    }

    VehicleGalleryChange retained(long revision,List<VehicleImage> images){return new VehicleGalleryChange(revision,images.stream().map(i->new VehicleGalleryChange.Entry(i.storageKey(),null,i.position(),i.primary())).toList(),List.of());}

    Long user(){return tx(()->{var now=Instant.now(clock);var u=users.create(User.create("Synthetic gallery actor",null,null,cpf(),null,null,null,now));accounts.create(AuthenticationAccount.create(u.getId(),"synthetic-unusable-password-hash",now));return u.getId();});}
    void grant(Long user,PermissionCode... codes){tx(()->{var now=Instant.now(clock);var role=roles.save(Role.create("Gallery authority "+UUID.randomUUID(),null,now));for(var code:codes){var permission=permissions.findByCode(code).orElseGet(()->permissions.save(Permission.fromDefinition(PermissionDefinition.from(code),now)));rolePermissions.save(RolePermission.create(role.getId(),permission.getId(),now));}userRoles.save(UserRole.create(user,role.getId(),now));return null;});}
    String token(Long user){var s=tx(()->sessions.save(Session.create(user,Duration.ofHours(1),Instant.now(clock))));String token="gallery-test-"+UUID.randomUUID();when(parser.parseAndValidate(token)).thenReturn(new JwtAuthenticatedUser(user,s.getId(),"Synthetic gallery actor",Instant.now(clock).plusSeconds(3600)));return "Bearer "+token;}
    <T>T tx(Supplier<T> work){return transactions.execute(status->work.get());}
    Long create(int count)throws Exception{var request=multipart("/vehicles/include/member").file(part("vehicle",data())).header("Authorization",ownerToken);for(int i=0;i<count;i++)request.file(photo(i));mvc.perform(request).andExpect(status().isCreated());return vehicles.findAll(owner,org.springframework.data.domain.PageRequest.of(0,50,org.springframework.data.domain.Sort.by("id").descending())).getContent().get(0).getId();}
    Long legacy(String photo){return tx(()->repository.save(Vehicle.create("Legacy",photo,plate(),renavam(),"Jeep","Legacy",2020,2021,"Blue",5,FuelType.DIESEL,2,true,owner,Instant.now(clock))).getId());}
    ResultActions detail(Long id,String bearer)throws Exception{return mvc.perform(get("/vehicles/detail/member/{id}",id).header("Authorization",bearer));}
    ResultActions replace(Long id,String bearer,String json)throws Exception{return mvc.perform(put("/vehicles/gallery/member/{id}",id).contentType("application/json").content(json).header("Authorization",bearer));}
    ResultActions uploadReplace(Long id,String bearer,String json,int files)throws Exception{return uploadReplace(id,bearer,json,files,false);}
    ResultActions uploadReplace(Long id,String bearer,String json,int files,boolean administrative)throws Exception{var r=multipart(HttpMethod.PUT,"/vehicles/gallery/"+(administrative?"admin":"member")+"/{id}",id).file(part("gallery",json)).header("Authorization",bearer);for(int i=0;i<files;i++)r.file(photo(i));return mvc.perform(r);}
    MockMultipartFile part(String name,String json){return new MockMultipartFile(name,"","application/json",json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    MockMultipartFile photo(int marker){var bytes=PNG.clone();bytes[8]=(byte)marker;return new MockMultipartFile("photos","photo.png","image/png",bytes);}
    String data(){return "{\"nickname\":\"Gallery\",\"plate\":\""+plate()+"\",\"renavam\":\""+renavam()+"\",\"brand\":\"Jeep\",\"model\":\"Gallery\",\"manufacturingYear\":2020,\"modelYear\":2021,\"color\":\"Blue\",\"seatingCapacity\":5,\"fuelType\":\"DIESEL\",\"engineDisplacement\":2,\"towing\":true}";}
    String payload(long revision,List<VehicleImage> images){return "{\"expectedRevision\":"+revision+",\"images\":["+images.stream().map(i->"{\"storageKey\":\""+i.storageKey()+"\",\"position\":"+i.position()+",\"primary\":"+i.primary()+"}").collect(java.util.stream.Collectors.joining(","))+"]}";}
    static String plate(){return "GAL%04d".formatted(NUMBERS.incrementAndGet()%10000);}
    static String renavam(){String base="%010d".formatted(NUMBERS.incrementAndGet());int[] weights={3,2,9,8,7,6,5,4,3,2};int sum=0;for(int i=0;i<10;i++)sum+=(base.charAt(i)-'0')*weights[i];int digit=sum*10%11;return base+(digit==10?0:digit);}
    static String cpf(){String value=Integer.toString(NUMBERS.incrementAndGet());for(int len=9;len<11;len++){int sum=0;for(int i=0;i<len;i++)sum+=(value.charAt(i)-'0')*(len+1-i);int mod=sum%11;value+=(mod<2?0:11-mod);}return value;}
    long files()throws Exception{try(var stream=Files.walk(PHOTO_ROOT)){return stream.filter(Files::isRegularFile).count();}}
    static void await(CountDownLatch latch){try{if(!latch.await(20,TimeUnit.SECONDS))throw new IllegalStateException("Synthetic gallery race timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}
