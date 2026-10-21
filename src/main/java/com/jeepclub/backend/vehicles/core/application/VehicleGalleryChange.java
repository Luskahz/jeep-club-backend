package com.jeepclub.backend.vehicles.core.application;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import java.util.*;

/** Complete desired gallery. File indices refer only to this request's uploads. */
public record VehicleGalleryChange(Long expectedRevision,List<Entry> images,List<ImageUpload> uploads) {
    public record Entry(String storageKey,Integer uploadIndex,int position,boolean primary) {
        public Entry {
            if((storageKey==null)==(uploadIndex==null) || position<0 || position>4)
                throw new IllegalArgumentException("Choose one stored reference or upload index and a position from zero to four.");
        }
        @Override public String toString() { return "GalleryEntry[position="+position+", primary="+primary+"]"; }
    }
    public VehicleGalleryChange {
        if(images==null || uploads==null || images.size()>5 || uploads.size()>5 || images.stream().anyMatch(Objects::isNull) || uploads.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Gallery requires zero to five images and uploads.");
        images=List.copyOf(images);uploads=List.copyOf(uploads);
        if(expectedRevision!=null && expectedRevision<0) throw new IllegalArgumentException("Invalid gallery revision.");
        if(!images.isEmpty() && images.stream().filter(Entry::primary).count()!=1)
            throw new IllegalArgumentException("Exactly one primary image is required.");
        Set<Integer> positions=new HashSet<>(),indices=new HashSet<>();Set<String> keys=new HashSet<>();
        for(var entry:images) {
            if(!positions.add(entry.position())) throw new IllegalArgumentException("Duplicate gallery position.");
            if(entry.storageKey()!=null && !keys.add(entry.storageKey())) throw new IllegalArgumentException("Duplicate gallery reference.");
            if(entry.uploadIndex()!=null && (entry.uploadIndex()<0 || entry.uploadIndex()>=uploads.size() || !indices.add(entry.uploadIndex())))
                throw new IllegalArgumentException("Invalid or duplicate upload index.");
        }
        for(int i=0;i<images.size();i++) if(!positions.contains(i)) throw new IllegalArgumentException("Gallery positions must be contiguous from zero.");
        if(indices.size()!=uploads.size()) throw new IllegalArgumentException("Every uploaded image must be referenced exactly once.");
    }
    public static VehicleGalleryChange uploads(List<ImageUpload> uploads) {
        var entries=new ArrayList<Entry>();for(int i=0;i<uploads.size();i++) entries.add(new Entry(null,i,i,i==0));
        return new VehicleGalleryChange(null,entries,uploads);
    }
    @Override public String toString() { return "VehicleGalleryChange[expectedRevision="+expectedRevision+", size="+images.size()+"]"; }
}
