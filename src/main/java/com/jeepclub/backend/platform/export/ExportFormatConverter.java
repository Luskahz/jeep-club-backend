package com.jeepclub.backend.platform.export;
import com.jeepclub.backend.shared.export.ExportFormat;
@org.springframework.stereotype.Component
public class ExportFormatConverter implements org.springframework.core.convert.converter.Converter<String,ExportFormat> {
    public ExportFormat convert(String value){return ExportFormat.valueOf(value.toUpperCase(java.util.Locale.ROOT));}
}
