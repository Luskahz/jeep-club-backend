package com.jeepclub.backend.platform.export;
import com.jeepclub.backend.shared.export.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.*;
import java.time.Instant;
import java.util.*;
/** A4 report with a repeated masthead and readable field/value tables, including multi-page fields. */
final class PdfExportWriter implements AutoCloseable {
    private final PDDocument pdf = new PDDocument();
    private final PDType0Font font;
    private final ExportDocument document;
    private final Instant generated;
    private PDPageContentStream stream;
    private float y;
    private String group;
    private int rows;
    PdfExportWriter(ExportDocument document, Instant generated) throws IOException {
        this.document=document; this.generated=generated;
        try (var input = PDType0Font.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
            font=PDType0Font.load(pdf,input);
        }
        pdf.getDocumentInformation().setTitle(document.title());
        pdf.getDocumentInformation().setCreator("Jeep Club");
        var date=GregorianCalendar.from(generated.atZone(java.time.ZoneOffset.UTC));
        pdf.getDocumentInformation().setCreationDate(date);
        page();
        for (String filter : document.filters()) text("Filtro: " + filter, 10, 500);
        y-=12;
    }
    private void page() throws IOException {
        if (pdf.getNumberOfPages()>=1000) throw new ExportException(ExportException.Reason.LIMIT);
        if(stream!=null) stream.close();
        var page=new PDPage(PDRectangle.A4); pdf.addPage(page);
        stream=new PDPageContentStream(pdf,page); y=790;
        draw("JEEP CLUB | " + document.title(),40,y,13); y-=20;
        draw("Gerado em " + ExportValues.text(generated),40,y,9); y-=24;
        draw("Página " + pdf.getNumberOfPages(),40,28,9);
    }
    void row(ExportRow row) throws IOException {
        if (row.group()!=null && !Objects.equals(group,row.group())) {
            group=row.group(); if(y<130) page(); text(group,12,500); y-=8;
        }
        if(y<120) page();
        text("Registro " + (++rows),11,500);
        for(int i=0;i<row.cells().size();i++) {
            if(row.group()!=null && row.groupedHeaderColumns().contains(i))continue;
            var values=wrap(row.cells().get(i).isEmpty()?"—":row.cells().get(i),335,10);
            var labels=wrap(document.columns().get(i),155,10);
            int count=Math.max(values.size(),labels.size());
            for(int line=0;line<count;line++) {
                if(y<60) page();
                if(line<labels.size()) draw(labels.get(line),42,y,10);
                if(line<values.size()) draw(values.get(line),205,y,10);
                y-=14;
            }
            y-=4;
        }
        stream.setStrokingColor(0.8f);stream.moveTo(40,y);stream.lineTo(555,y);stream.stroke();y-=16;
    }
    private void text(String value,int size,int width) throws IOException {
        for(String line:wrap(value,width,size)) { if(y<60)page();draw(line,40,y,size);y-=size+5; }
    }
    private String safe(String value) throws IOException {
        var out=new StringBuilder();
        for(int cp:value.codePoints().toArray()) {
            if(cp=='\n') {out.append('\n');continue;}
            if(Character.isISOControl(cp)) {out.append(' ');continue;}
            String s=new String(Character.toChars(cp));
            try {font.encode(s);out.append(s);}catch(IllegalArgumentException e){out.append('?');}
        }
        return out.toString();
    }
    private List<String> wrap(String value,int width,int size) throws IOException {
        var lines=new ArrayList<String>(); var line=new StringBuilder();
        for(int cp:safe(value).codePoints().toArray()) {
            if(cp=='\n') {lines.add(line.toString());line.setLength(0);continue;}
            String s=new String(Character.toChars(cp));
            if(font.getStringWidth(line+s)/1000*size>width && !line.isEmpty()) {lines.add(line.toString());line.setLength(0);}
            line.append(s);
        }
        lines.add(line.toString());return lines;
    }
    private void draw(String value,float x,float baseline,int size) throws IOException {
        stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,baseline);stream.showText(safe(value));stream.endText();
    }
    void save(OutputStream output) throws IOException {
        if(rows==0) text("Nenhum registro encontrado.",11,500);
        stream.close();stream=null;pdf.save(output);
    }
    public void close() throws IOException { if(stream!=null)stream.close();pdf.close(); }
}
