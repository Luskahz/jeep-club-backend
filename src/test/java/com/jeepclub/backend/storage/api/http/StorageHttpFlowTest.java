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

@SpringBootTest(properties="storage.media.enabled=true") @AutoConfigureMockMvc @ActiveProfiles("test")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class StorageHttpFlowTest {
    static final Path ROOT;
    static {try {ROOT=Files.createTempDirectory("storage-http-fixture-");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {r.add("storage.local.root-directory",() -> ROOT.toString());}
    @Autowired MockMvc mvc; @Autowired UserRegistration users; @Autowired UserQuery query; @Autowired MediaGovernance governance;
    @Test void authenticatedOwnerReadsMasterWhileOtherUsersAndModerationCannotBypassIt() throws Exception {
        var owner=users.registerAndAuthenticate(new UserRegistrationData("Storage fixture",null,"storage-fixture@example.com","52998224725",null,null,null,Instant.now()),"Password@123");
        var other=users.registerAndAuthenticate(new UserRegistrationData("Other fixture",null,"other-storage-fixture@example.com","16899535009",null,null,null,Instant.now()),"Password@123");
        var image=new BufferedImage(80,60,BufferedImage.TYPE_INT_RGB);var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);
        String response=mvc.perform(multipart("/storage/media/profile").file(new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray()))
            .header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.storageKey").doesNotExist()).andExpect(jsonPath("$.sha256").doesNotExist()).andReturn().getResponse().getContentAsString();
        long media=((Number)JsonPath.read(response,"$.mediaId")).longValue(),ref=((Number)JsonPath.read(response,"$.referenceId")).longValue();
        String url="/storage/media/"+media+"/references/"+ref+"/variants/MASTER";
        mvc.perform(get(url).header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","private, no-store"));
        mvc.perform(get(url).header("Authorization","Bearer "+other.accessToken())).andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/storage/media").header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isNotFound());
        long actor=query.findByCpf("52998224725").orElseThrow().id();
        governance.moderate(new ModerationDecision(media,null,ModerationDecision.State.BLOCKED,ModerationDecision.Reason.PRIVACY,"internal fixture",actor,Instant.now()));
        mvc.perform(get(url).header("Authorization","Bearer "+owner.accessToken()).header("If-None-Match","*"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.reasonCode").value("CONTENT_UNAVAILABLE"));
        mvc.perform(head(url).header("Authorization","Bearer "+owner.accessToken())).andExpect(status().isNotFound());
    }
}
