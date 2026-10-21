package com.jeepclub.backend.vehicles.core.application.service.internal;
import com.jeepclub.backend.vehicles.core.application.VehicleGalleryChange;
import com.jeepclub.backend.vehicles.core.domain.model.*;
import com.jeepclub.backend.platform.storage.image.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component @RequiredArgsConstructor
public class VehicleGalleryAssembler {
    private final ImageMediaService images;
    private final ImageMediaCommand mediaCommand;
    public VehicleGallery assemble(VehicleGallery current,VehicleGalleryChange change) {
        var retained=current.images().stream().map(VehicleImage::storageKey).collect(java.util.stream.Collectors.toSet());
        for(var entry:change.images()) if(entry.storageKey()!=null && !retained.contains(entry.storageKey())) images.requireExisting(entry.storageKey());
        var values=new ArrayList<VehicleImage>();
        for(var entry:change.images()) {
            String key=entry.storageKey()!=null?entry.storageKey():mediaCommand.storeInTransaction(change.uploads().get(entry.uploadIndex()));
            values.add(entry.uploadIndex()==null?new VehicleImage(key,entry.position(),entry.primary()):VehicleImage.newReference(key,entry.position(),entry.primary()));
        }
        return VehicleGallery.of(values);
    }
}
