package com.jeepclub.backend.shared.export;
import java.util.*;
/** Text cells shared by CSV and PDF. Grouped PDF tables may suppress fields already in the heading. */
public record ExportRow(String group, List<String> cells, Set<Integer> groupedHeaderColumns) {
    public ExportRow { cells=List.copyOf(cells);groupedHeaderColumns=Set.copyOf(groupedHeaderColumns); }
    public ExportRow(String group,List<String> cells){this(group,cells,Set.of());}
    public static ExportRow of(Object... cells){return new ExportRow(null,Arrays.stream(cells).map(ExportValues::text).toList());}
    public ExportRow grouped(String heading){return new ExportRow(heading,cells);}
    public ExportRow grouped(String heading,Integer... columns){return new ExportRow(heading,cells,Set.of(columns));}
}
