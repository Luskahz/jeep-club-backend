package com.jeepclub.backend.vehicles.api.http.dto.gallery;
import com.jeepclub.backend.platform.storage.image.*;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import com.jeepclub.backend.vehicles.core.application.VehicleGalleryChange;
import org.springframework.web.multipart.*;
import java.io.IOException;
import java.util.*;
public final class VehicleGalleryHttp {
    private VehicleGalleryHttp() {}
    public static void validate(MultipartHttpServletRequest request,boolean creation) throws IOException, jakarta.servlet.ServletException {
        // Servlet form fields and file parts live in separate maps. Count the original
        // JSON parts so a duplicate split across those maps cannot bypass either limit.
        var structuredCounts=new HashMap<String,Integer>();
        for(var part:request.getParts()) {
            if(part.getName().equals("gallery") || creation && part.getName().equals("vehicle"))
                if(structuredCounts.merge(part.getName(),1,Integer::sum)>1)
                    throw new InvalidStorageFileException("Structured JSON parts must not be repeated.");
        }
        for(var e:request.getMultiFileMap().entrySet()) {
            int limit=e.getKey().equals("photos")?5:(e.getKey().equals("gallery") || creation && (e.getKey().equals("vehicle") || e.getKey().equals("photo")))?1:0;
            if(e.getValue().size()>limit) throw new InvalidStorageFileException("Unexpected multipart fields or image count.");
        }
        request.getParameterMap().forEach((name,values) -> {
            if(values.length!=1 || !(name.equals("gallery") || creation && name.equals("vehicle")))
                throw new InvalidStorageFileException("Send structured JSON parts and image files only.");
        });
        if(request.getMultiFileMap().containsKey("photo") && (request.getMultiFileMap().containsKey("photos") || request.getMultiFileMap().containsKey("gallery") || request.getParameterMap().containsKey("gallery")))
            throw new InvalidStorageFileException("Legacy photo cannot be combined with gallery/photos.");
    }
    public static VehicleGalleryChange change(VehicleGalleryRequest gallery,List<MultipartFile> files) throws IOException {
        var uploads=new ArrayList<ImageUpload>();
        if(files!=null) {
            if(files.size()>5) throw new InvalidStorageFileException("A vehicle accepts at most five images.");
            for(var file:files) uploads.add(ImageMultipartAdapter.read(file));
        }
        return gallery==null?VehicleGalleryChange.uploads(uploads):gallery.change(uploads);
    }
}
