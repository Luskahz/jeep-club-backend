package com.jeepclub.backend.storage.core.repository;

public interface LegacyMediaSource {
    record Metadata(String mimeType,Integer width,Integer height) {}
    byte[] read(String key);
    default Metadata identify(byte[] bytes) {return new Metadata("application/octet-stream",null,null);}
}
