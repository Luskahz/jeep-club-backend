package com.jeepclub.backend.storage.api.http;

import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.storage.core.repository.MediaGovernance;
import com.jeepclub.backend.storage.core.domain.model.ModerationDecision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.jayway.jsonpath.JsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"storage.media.enabled=true","storage.legacy-backfill.enabled=true"}) @AutoConfigureMockMvc @ActiveProfiles("test")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class StorageHttpFlowTest {
    static final Path ROOT;
    static {try {ROOT=Files.createTempDirectory("storage-http-fixture-");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {r.add("storage.local.root-directory",() -> ROOT.toString());}
    @Autowired MockMvc mvc; @Autowired UserRegistration users; @Autowired UserQuery query; @Autowired MediaGovernance governance;
    @Autowired com.jeepclub.backend.storage.core.application.MediaReferenceService references;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    @Autowired com.jeepclub.backend.storage.core.repository.LegacyBackfill backfill;
    @Autowired com.jeepclub.backend.storage.core.repository.MediaCatalogRepository catalog;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.jeepclub.backend.storage.core.repository.LegacyMediaSource legacySource;

    @Test void legacyHttpResponsesAreSanitizedAndDeniedRequestsNeverReadProvider() throws Exception {
        var owner=users.registerAndAuthenticate(new UserRegistrationData("Legacy owner",null,"legacy-owner@example.com","11144477735",null,null,null,Instant.now()),"Password@123");
        var other=users.registerAndAuthenticate(new UserRegistrationData("Legacy other",null,"legacy-other@example.com","93541134780",null,null,null,Instant.now()),"Password@123");
        long actor=query.findByCpf("11144477735").orElseThrow().id();
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(12,9,BufferedImage.TYPE_INT_RGB),"png",out);byte[] bytes=out.toByteArray();
        String key="images/2026/11/12/550e8400-e29b-41d4-a716-446655440002.png";
        Path file=ROOT.resolve(key);Files.createDirectories(file.getParent());Files.write(file,bytes);
        var audit=new com.jeepclub.backend.storage.core.domain.model.MediaAuditContext(com.jeepclub.backend.storage.core.domain.enums.MediaActorKind.SYSTEM,null,Instant.now(),null);
        long media=backfill.apply(key,audit).mediaId();
        var ref=catalog.recordReference(new com.jeepclub.backend.storage.core.domain.model.MediaReference(null,media,
            com.jeepclub.backend.storage.core.domain.enums.MediaContext.IDENTITY_PROFILE,Long.toString(actor),"PHOTO",null,
            com.jeepclub.backend.storage.core.domain.enums.MediaOrigin.LEGACY_IMPORT,audit.occurredAt(),null,null,null,null,null),audit);
        String url="/storage/media/"+media+"/references/"+ref.referenceId()+"/variants/MASTER";
        mvc.perform(get(url).header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isOk()).andExpect(content().bytes(bytes));
        org.mockito.Mockito.clearInvocations(legacySource);
        assertSanitized(url,other.accessToken(),404);
        org.mockito.Mockito.verify(legacySource,org.mockito.Mockito.never()).read(org.mockito.ArgumentMatchers.anyString());
        Path absent=file.resolveSibling("temporarily-unavailable.png");Files.move(file,absent);
        assertSanitized(url,owner.accessToken(),404);
        org.mockito.Mockito.clearInvocations(legacySource);
        assertSanitized(url,other.accessToken(),404);
        org.mockito.Mockito.verify(legacySource,org.mockito.Mockito.never()).read(org.mockito.ArgumentMatchers.anyString());
        Files.move(absent,file);Files.write(file,new byte[]{1,2,3});
        assertSanitized(url,owner.accessToken(),503);
        Files.write(file,bytes);
        org.mockito.Mockito.doThrow(new com.jeepclub.backend.shared.storage.exception.StorageOperationException(
            com.jeepclub.backend.shared.storage.exception.StorageOperationException.Operation.READ,new java.io.IOException("secret path /private-images/fixture")))
            .when(legacySource).read(key);
        try {assertSanitized(url,owner.accessToken(),503);}
        finally {org.mockito.Mockito.doCallRealMethod().when(legacySource).read(key);}
        governance.moderate(new ModerationDecision(media,null,ModerationDecision.State.BLOCKED,ModerationDecision.Reason.PRIVACY,"private",actor,Instant.now()));
        org.mockito.Mockito.clearInvocations(legacySource);assertSanitized(url,owner.accessToken(),404);
        org.mockito.Mockito.verify(legacySource,org.mockito.Mockito.never()).read(org.mockito.ArgumentMatchers.anyString());
    }
    private void assertSanitized(String url,String token,int expected) throws Exception {
        var result=mvc.perform(get(url).header("Authorization","Bearer "+token)).andExpect(status().is(expected))
            .andExpect(header().string("Cache-Control","private, no-store")).andReturn();
        String json=result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json).doesNotContain("storageKey","sha256","SHA-256","namespace","private-images","fixture","secret path");
    }
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="storage.benchmark",matches="true")
    void benchmarkConcurrentProfileUploads() throws Exception {
        var user=users.registerAndAuthenticate(new UserRegistrationData("Benchmark fixture",null,"benchmark-storage@example.com","52998224725",null,null,null,Instant.now()),"Password@123");
        var corpus=new java.util.ArrayList<byte[]>();
        for(int i=0;i<22;i++) {
            var image=new BufferedImage(1024,768,BufferedImage.TYPE_INT_RGB);
            var random=new java.util.Random(4711L+i);
            int[] pixels=new int[1024*768];for(int p=0;p<pixels.length;p++) pixels[p]=random.nextInt(0x1000000);
            image.setRGB(0,0,1024,768,pixels,0,1024);
            var encoded=new ByteArrayOutputStream();ImageIO.write(image,"png",encoded);corpus.add(encoded.toByteArray());
        }
        for(int i=0;i<2;i++) {
            mvc.perform(multipart("/storage/media/profile").file(new MockMultipartFile("file","fixture.png","image/png",corpus.get(i)))
                .header("Authorization","Bearer "+user.accessToken())).andExpect(status().isCreated());
        }
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        var futures=new java.util.ArrayList<java.util.concurrent.Future<Long>>();
        try {
            for(int i=2;i<corpus.size();i++) {
                byte[] fixture=corpus.get(i);
                futures.add(pool.submit(() -> {
                    start.await();long begin=System.nanoTime();
                    mvc.perform(multipart("/storage/media/profile").file(new MockMultipartFile("file","fixture.png","image/png",fixture))
                        .header("Authorization","Bearer "+user.accessToken())).andExpect(status().isCreated());
                    return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin);
                }));
            }
            start.countDown();var durations=new java.util.ArrayList<Long>();
            for(var future:futures) durations.add(future.get(120,java.util.concurrent.TimeUnit.SECONDS));
            java.util.Collections.sort(durations);long p95=durations.get((int)Math.ceil(durations.size()*.95)-1);
            System.out.println("STORAGE_BENCHMARK samples="+durations.size()+" concurrency=2 warmup=2 input=1024x768-random-png bytes="+corpus.get(0).length
                    +" p95_ms="+p95+" max_ms="+durations.get(durations.size()-1)+" os="+System.getProperty("os.name")
                    +" java="+System.getProperty("java.version")+" cpus="+Runtime.getRuntime().availableProcessors());
            org.assertj.core.api.Assertions.assertThat(p95).isLessThan(10_000);
        } finally {pool.shutdownNow();}
    }
    @Test void authenticatedOwnerReadsMasterWhileOtherUsersAndModerationCannotBypassIt() throws Exception {
        var owner=users.registerAndAuthenticate(new UserRegistrationData("Storage fixture",null,"storage-fixture@example.com","52998224725",null,null,null,Instant.now()),"Password@123");
        var other=users.registerAndAuthenticate(new UserRegistrationData("Other fixture",null,"other-storage-fixture@example.com","16899535009",null,null,null,Instant.now()),"Password@123");
        var image=new BufferedImage(80,60,BufferedImage.TYPE_INT_RGB);var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);
        byte[] webp=dev.matrixlab.webp4j.WebPCodec.encodeLosslessImage(image);
        mvc.perform(multipart("/storage/media/profile").file(new MockMultipartFile("file","photo.webp","image/webp",webp))
            .header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isCreated());
        mvc.perform(multipart("/storage/media/profile").file(new MockMultipartFile("file","photo.webp","image/webp",java.util.Arrays.copyOf(webp,webp.length-1)))
            .header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isBadRequest());
        var options=new MockMultipartFile("options","","application/json",
            "{\"crops\":{\"AVATAR_SMALL\":{\"x\":0,\"y\":0,\"width\":0.75,\"height\":1},\"CARD_PORTRAIT\":{\"x\":0,\"y\":0,\"width\":0.375,\"height\":1}}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String response=mvc.perform(multipart("/storage/media/profile").file(new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray()))
            .file(options)
            .header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.storageKey").doesNotExist()).andExpect(jsonPath("$.sha256").doesNotExist()).andReturn().getResponse().getContentAsString();
        long media=((Number)JsonPath.read(response,"$.mediaId")).longValue();
        org.assertj.core.api.Assertions.assertThat((Object)JsonPath.read(response,"$.referenceId")).isNull();
        long actor=query.findByCpf("52998224725").orElseThrow().id();
        mvc.perform(post("/storage/media/"+media+"/references").header("Authorization","Bearer "+owner.accessToken())
            .contentType("application/json").content("{\"context\":\"IDENTITY_PROFILE\",\"consumerId\":\""+actor+"\",\"role\":\"PHOTO\"}"))
            .andExpect(status().isNotFound());
        // Explicit reference fixture: this is resolver coverage, not consumer integration proof.
        long ref=new org.springframework.transaction.support.TransactionTemplate(manager).execute(s -> references.associate(actor,media,
            com.jeepclub.backend.storage.core.domain.enums.MediaContext.IDENTITY_PROFILE,Long.toString(actor),"PHOTO"));
        var avatar=mvc.perform(get("/storage/media/"+media+"/references/"+ref+"/variants/AVATAR_SMALL")
            .header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var decoded=avatar.length>12 && avatar[8]=='W' && avatar[9]=='E' && avatar[10]=='B' && avatar[11]=='P'
            ? dev.matrixlab.webp4j.WebPCodec.decodeImage(avatar) : ImageIO.read(new java.io.ByteArrayInputStream(avatar));
        org.assertj.core.api.Assertions.assertThat(decoded.getWidth()).isEqualTo(decoded.getHeight());
        var card=mvc.perform(get("/storage/media/"+media+"/references/"+ref+"/variants/CARD_PORTRAIT")
            .header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var cardImage=card.length>12 && card[8]=='W' && card[9]=='E' && card[10]=='B' && card[11]=='P'
            ? dev.matrixlab.webp4j.WebPCodec.decodeImage(card) : ImageIO.read(new java.io.ByteArrayInputStream(card));
        org.assertj.core.api.Assertions.assertThat(cardImage.getWidth()).isLessThan(cardImage.getHeight());
        var invalid=new MockMultipartFile("options","","application/json",
            "{\"crops\":{\"AVATAR_SMALL\":{\"x\":0.9,\"y\":0,\"width\":0.5,\"height\":1}}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(multipart("/storage/media/profile").file(new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray()))
            .file(invalid).header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isBadRequest());
        String url="/storage/media/"+media+"/references/"+ref+"/variants/MASTER";
        mvc.perform(get(url).header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","private, no-store"));
        mvc.perform(get(url).header("Authorization","Bearer "+other.accessToken())).andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/storage/media").header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isNotFound());
        governance.moderate(new ModerationDecision(media,null,ModerationDecision.State.BLOCKED,ModerationDecision.Reason.PRIVACY,"internal fixture",actor,Instant.now()));
        mvc.perform(get(url).header("Authorization","Bearer "+owner.accessToken()).header("If-None-Match","*"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.reasonCode").value("CONTENT_UNAVAILABLE"));
        mvc.perform(head(url).header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isNotFound());
    }
}
