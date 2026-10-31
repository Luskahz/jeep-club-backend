package com.jeepclub.backend.vehicles.api.http.dto.gallery;

import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockPart;

import static org.assertj.core.api.Assertions.*;

class VehicleGalleryHttpTest {
    @Test void aSingleJsonPartRepresentedInBothMapsIsNotCountedTwice() {
        var request = new MockMultipartHttpServletRequest();
        request.addFile(new MockMultipartFile("vehicle", "", "application/json", "{}".getBytes()));
        request.addParameter("vehicle", "{}");
        request.addPart(new MockPart("vehicle", "{}".getBytes()));
        assertThatCode(() -> VehicleGalleryHttp.validate(request, true)).doesNotThrowAnyException();
    }

    @Test void twoOriginalJsonPartsAreRejectedEvenWhenSplitBetweenTheMaps() {
        var request = new MockMultipartHttpServletRequest();
        request.addFile(new MockMultipartFile("gallery", "data.json", "application/json", "{}".getBytes()));
        request.addParameter("gallery", "{}");
        request.addPart(new MockPart("gallery", "{}".getBytes()));
        request.addPart(new MockPart("gallery", "data.json", "{}".getBytes()));
        assertThatThrownBy(() -> VehicleGalleryHttp.validate(request, false)).isInstanceOf(InvalidStorageFileException.class);
    }
}
