package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.vehicles.core.domain.enums.*;
import com.jeepclub.backend.vehicles.core.domain.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class VehicleGalleryTest {
    private static final Instant NOW=Instant.parse("2026-10-19T00:00:00Z");
    private static String key(int i) { return "images/2026/10/19/00000000-0000-0000-0000-%012d.png".formatted(i); }
    private static VehicleImage image(int i,int p,boolean primary) { return VehicleImage.newReference(key(i),p,primary); }
    private Vehicle vehicle() { return Vehicle.create("Gallery",null,"ABC1D23","38249206428","Jeep","Test",2020,2021,"Blue",5,FuelType.DIESEL,2,true,1L,NOW); }

    @ParameterizedTest @ValueSource(ints={0,1,5}) void supportsEmptySingleAndMaximum(int count) {
        var images=new ArrayList<VehicleImage>();for(int i=0;i<count;i++)images.add(image(i,i,i==0));
        var gallery=VehicleGallery.of(images);assertThat(gallery.images()).hasSize(count);
        assertThat(gallery.primaryReference()).isEqualTo(count==0?null:key(0));
        images.clear();assertThat(gallery.images()).hasSize(count);
        assertThatThrownBy(()->gallery.images().add(image(8,0,true))).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void rejectsSixBeforeAnyAggregateChange() {
        var six=new ArrayList<VehicleImage>();for(int i=0;i<6;i++)six.add(new VehicleImage(key(i),i%5,i==0));
        assertThatThrownBy(()->VehicleGallery.of(six)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void requiresOnePrimary() {
        assertThatThrownBy(()->VehicleGallery.of(List.of(image(0,0,false)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->VehicleGallery.of(List.of(image(0,0,true),image(1,1,true)))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsDuplicateReferencesPositionsAndGaps() {
        assertThatThrownBy(()->VehicleGallery.of(List.of(image(0,0,true),image(0,1,false)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->VehicleGallery.of(List.of(image(0,0,true),image(1,0,false)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->VehicleGallery.of(List.of(image(0,0,true),image(1,2,false)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->VehicleGallery.of(Arrays.asList((VehicleImage)null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->VehicleGallery.of(null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sortsStableAndPreservesLegacyWithoutApplyingNewUploadRules() {
        var g=VehicleGallery.of(List.of(image(1,1,true),image(0,0,false)));
        assertThat(g.images()).extracting(VehicleImage::position).containsExactly(0,1);
        assertThat(g.primaryReference()).isEqualTo(key(1));
        assertThat(VehicleGallery.legacy("https://legacy.invalid/Photo.PNG?old=1").primaryReference()).isEqualTo("https://legacy.invalid/Photo.PNG?old=1");
        assertThat(VehicleGallery.legacy("").images()).hasSize(1);
        String unicodeLegacy="https://legacy.invalid/"+"\uD83D\uDE99".repeat(300);
        assertThat(VehicleGallery.legacy(unicodeLegacy).primaryReference()).isEqualTo(unicodeLegacy);
        assertThatThrownBy(()->VehicleImage.newReference("https://legacy.invalid/a.png",0,true)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->VehicleImage.newReference("private-images/2026/10/19/00000000-0000-0000-0000-000000000001.png",0,true)).isInstanceOf(RuntimeException.class);
    }
    @Test void revisionProtectsAgainstLostChangesAndExactRepeatIsIdempotent() {
        var v=vehicle();var one=VehicleGallery.of(List.of(image(0,0,true)));
        assertThat(v.replaceGallery(one,0,NOW.plusSeconds(1))).isTrue();assertThat(v.getGalleryRevision()).isEqualTo(1);
        assertThat(v.replaceGallery(one,0,NOW.plusSeconds(2))).isFalse();assertThat(v.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(()->v.replaceGallery(VehicleGallery.empty(),0,NOW.plusSeconds(3))).isInstanceOf(VehicleGalleryConflictException.class);
        assertThat(v.getGallery()).isEqualTo(one);assertThat(v.getGalleryRevision()).isEqualTo(1);
        assertThatThrownBy(()->v.assertLegacyPhotoChangeAllowed(key(2))).isInstanceOf(VehicleGalleryConflictException.class);
        assertThatCode(()->v.assertLegacyPhotoChangeAllowed(key(0))).doesNotThrowAnyException();
        assertThat(v.replaceGallery(VehicleGallery.empty(),1,NOW.plusSeconds(4))).isTrue();assertThat(v.getPhoto()).isNull();
    }
    @Test void lifecycleBlocksMutationAndLegacyAliasDoesNotEraseMultipleImages() {
        var v=vehicle();var gallery=VehicleGallery.of(List.of(image(0,0,true),image(1,1,false)));v.initializeGallery(gallery);
        assertThatThrownBy(()->v.update("Gallery",null,v.getPlate(),v.getRenavam(),v.getBrand(),v.getModel(),2020,2021,"Blue",5,FuelType.DIESEL,2,true,NOW.plusSeconds(1))).isInstanceOf(VehicleGalleryConflictException.class);
        assertThat(v.getGallery()).isEqualTo(gallery);
        v.archive(NOW.plusSeconds(2));
        assertThatThrownBy(()->v.replaceGallery(VehicleGallery.empty(),0,NOW.plusSeconds(3))).isInstanceOf(VehicleLifecycleConflictException.class);
    }
}
