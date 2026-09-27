package com.jeepclub.backend.publications.api.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicationSocialFeedOpenApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Test void socialAndFeedContractsExposePermissionsAndSchemas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/publications/{publicationId}/likes']['post']['x-required-permissions'][0]").value("PUBLICATIONS_INTERACTION_LIKE"))
                .andExpect(jsonPath("$['paths']['/publications/{publicationId}/likes/me']['delete']['x-required-permissions'][0]").value("PUBLICATIONS_INTERACTION_LIKE"))
                .andExpect(jsonPath("$['paths']['/publications/{publicationId}/comments']['post']['x-required-permissions'][0]").value("PUBLICATIONS_COMMENT_CREATE"))
                .andExpect(jsonPath("$['paths']['/publications/{publicationId}/comments']['get']['x-required-permissions'][0]").value("PUBLICATIONS_COMMENT_READ"))
                .andExpect(jsonPath("$['paths']['/publications/feed']['get']['x-required-permissions'][0]").value("PUBLICATIONS_FEED_READ"))
                .andExpect(jsonPath("$['paths']['/publications/{publicationId}']['get']['x-required-permissions'][0]").value("PUBLICATIONS_PUBLICATION_READ"))
                .andExpect(jsonPath("$['paths']['/publications/feed']['get']['responses']['200']").exists())
                .andExpect(jsonPath("$['paths']['/publications/{publicationId}/comments']['post']['responses']['201']").exists());
    }
}
