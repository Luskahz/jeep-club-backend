package com.jeepclub.backend.shared.export;
public record ExportFile(String filename, String contentType, byte[] bytes) {}
