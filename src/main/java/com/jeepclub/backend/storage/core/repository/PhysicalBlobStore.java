package com.jeepclub.backend.storage.core.repository;

import java.util.Set;

public interface PhysicalBlobStore {
    void putVerified(String digest, byte[] bytes);
    byte[] readVerified(String digest, long size);
    Set<String> inventory();
}
