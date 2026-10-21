package com.jeepclub.backend.vehicles.core.domain.model;

import java.util.*;

/** Immutable, complete gallery; partial invalid states are never assigned to Vehicle. */
public final class VehicleGallery {
    private final List<VehicleImage> images;
    private VehicleGallery(List<VehicleImage> images) { this.images=images; }
    public static VehicleGallery empty() { return new VehicleGallery(List.of()); }
    public static VehicleGallery legacy(String photo) {
        return photo==null?empty():of(List.of(new VehicleImage(photo,0,true)));
    }
    public static VehicleGallery of(List<VehicleImage> values) {
        if(values==null || values.size()>5 || values.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Vehicle gallery requires zero to five non-null images.");
        if(values.isEmpty()) return empty();
        if(values.stream().filter(VehicleImage::primary).count()!=1)
            throw new IllegalArgumentException("Non-empty vehicle gallery requires exactly one primary image.");
        Set<Integer> positions=new HashSet<>();Set<String> keys=new HashSet<>();
        for(var image:values) if(!positions.add(image.position()) || !keys.add(image.storageKey()))
            throw new IllegalArgumentException("Vehicle image references and positions must be unique.");
        for(int i=0;i<values.size();i++) if(!positions.contains(i))
            throw new IllegalArgumentException("Vehicle image positions must be contiguous from zero.");
        return new VehicleGallery(values.stream().sorted(Comparator.comparingInt(VehicleImage::position)).toList());
    }
    public List<VehicleImage> images() { return images; }
    public String primaryReference() { return images.stream().filter(VehicleImage::primary).map(VehicleImage::storageKey).findFirst().orElse(null); }
    @Override public boolean equals(Object other) { return other instanceof VehicleGallery g && images.equals(g.images); }
    @Override public int hashCode() { return images.hashCode(); }
    @Override public String toString() { return "VehicleGallery[size="+images.size()+"]"; }
}
