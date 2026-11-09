package com.jeepclub.backend.storage.core.repository;

import java.util.List;

public interface PhysicalBlobStore {
    record InventoryPage(List<String> filenames,String nextCursor) {public InventoryPage {filenames=List.copyOf(filenames);}}
    void putVerified(String digest,byte[] bytes);
    byte[] readVerified(String digest,long size);
    InventoryPage inventory(String cursor,int limit);
}
