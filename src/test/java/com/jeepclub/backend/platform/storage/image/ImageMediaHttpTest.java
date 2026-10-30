package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import com.jeepclub.backend.shared.storage.*;
import com.jeepclub.backend.shared.storage.exception.StorageOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(OutputCaptureExtension.class)
class ImageMediaHttpTest {
    private static final byte[] PNG = {(byte)137,80,78,71,13,10,26,10,1};
    private final FileStorage storage = mock(FileStorage.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ImageMediaController(new ImageMediaService(storage)))
            .setControllerAdvice(new ImageMediaExceptionHandler(), new GlobalExceptionHandler()).build();

    @Test void readFailureIsControlledAndDoesNotReachStorageOrLogs(CapturedOutput output) throws Exception {
        var file = spy(new MockMultipartFile("file", "sensitive-filename.png", "image/png", PNG));
        doThrow(new IOException("sensitive-path-storageKey-bytes")).when(file).getBytes();
        mvc.perform(multipart("/media/images").file(file)).andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(500)).andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_READ_FAILED"))
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).doesNotContain("sensitive-", "IOException", "stackTrace"));
        verifyNoInteractions(storage);
        assertThat(output.getAll()).doesNotContain("sensitive-", "java.io.IOException");
    }

    @Test void validUploadAndStorageWriteFailureKeepTheirSeparateContracts() throws Exception {
        when(storage.store(any(), eq("images"))).thenReturn(new StoredFile("images/2026/10/29/550e8400-e29b-41d4-a716-446655440000.png"));
        mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "image.png", "image/png", PNG)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.storageKey").isNotEmpty());
        when(storage.store(any(), eq("images"))).thenThrow(new StorageOperationException(StorageOperationException.Operation.WRITE, new IOException("sensitive-write-path")));
        mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "image.png", "image/png", PNG)))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("IMAGE_STORAGE_UNAVAILABLE"))
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).doesNotContain("sensitive-", "IMAGE_UPLOAD_READ_FAILED"));
    }

    @Test void invalidContentAndMissingPartStillReturn400AndWrongRequestType415() throws Exception {
        for (var file : new MockMultipartFile[]{
                new MockMultipartFile("file", "image.png", "image/png", new byte[0]),
                new MockMultipartFile("file", "image.png", "image/png", new byte[ImageUpload.MAX_BYTES + 1]),
                new MockMultipartFile("file", "image.png", "image/jpeg", PNG),
                new MockMultipartFile("file", "image.png", "image/png", new byte[]{1,2,3})}) {
            mvc.perform(multipart("/media/images").file(file)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        }
        mvc.perform(multipart("/media/images")).andExpect(status().isBadRequest());
        mvc.perform(post("/media/images").contentType("application/json").content("{}")).andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(storage);
    }
}
