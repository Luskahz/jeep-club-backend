package com.jeepclub.backend.publications.core.domain.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** The media references proposed by a request or associated with a publication. */
public final class PublicationGallery {
    private final List<PublicationImage> images;

    private PublicationGallery(List<PublicationImage> images) {
        this.images = images;
    }

    public static PublicationGallery of(List<PublicationImage> input) {
        if (input == null || input.isEmpty() || input.size() > 5) {
            throw new IllegalArgumentException("Gallery requires 1 to 5 images.");
        }
        List<PublicationImage> copy = List.copyOf(input);
        if (copy.stream().filter(PublicationImage::primary).count() != 1) {
            throw new IllegalArgumentException("Gallery requires exactly one primary image.");
        }
        var positions = new HashSet<Integer>();
        var keys = new HashSet<String>();
        for (PublicationImage image : copy) {
            if (!positions.add(image.position()) || !keys.add(image.storageKey())) {
                throw new IllegalArgumentException("Image positions and keys must be unique.");
            }
        }
        for (int position = 0; position < copy.size(); position++) {
            if (!positions.contains(position)) throw new IllegalArgumentException("Image positions must be contiguous from zero.");
        }
        return new PublicationGallery(copy.stream().sorted(Comparator.comparingInt(PublicationImage::position)).toList());
    }

    public List<PublicationImage> images() { return images; }
}
