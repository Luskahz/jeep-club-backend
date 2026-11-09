package com.jeepclub.backend.storage.core.repository;

import java.util.List;

/** Internal read-only custody report. Physical identifiers are not HTTP representations. */
public interface BlobInspection {
    record Item(String digest,String state,long logicalVariants,boolean shared) {}
    record Report(List<Item> items,List<String> uncataloguedFiles,String nextBlobCursor,String nextInventoryCursor) {}
    default Report inspect(String afterDigest,int limit) {return inspect(afterDigest,limit,null,false);}
    Report inspect(String afterDigest,int limit,String inventoryCursor,boolean includeFilesystem);
}
