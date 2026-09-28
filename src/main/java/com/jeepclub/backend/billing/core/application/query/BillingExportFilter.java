package com.jeepclub.backend.billing.core.application.query;
import java.time.*;
import com.jeepclub.backend.shared.export.*;
public record BillingExportFilter(Long id,Long userId,Long chargeDefinitionId,Long chargeCycleId,Long eventId,
    String recurrence,Integer year,Integer month,String status,String paymentMethod,String chargeStatus,String effectiveStatus,
    LocalDate dueFrom,LocalDate dueTo,Instant from,Instant to) {
    public void validate() {
        for(Long id:java.util.Arrays.asList(id,userId,chargeDefinitionId,chargeCycleId,eventId))if(id!=null && id<=0)invalid();
        if(month!=null && (month<1 || month>12 || year==null) || year!=null && (year<1900 || year>9999) || dueFrom!=null && dueTo!=null && dueFrom.isAfter(dueTo) || from!=null && to!=null && from.isAfter(to)) invalid();
        if(recurrence!=null)check(recurrence,"ONE_TIME MONTHLY YEARLY");
        if(effectiveStatus!=null)check(effectiveStatus,"PENDING OVERDUE EXPIRED PAID CANCELED");
        if(chargeStatus!=null)check(chargeStatus,"PENDING PAID CANCELED");
    }
    public static void check(String value,String allowed) { if(value!=null && !java.util.List.of(allowed.split(" ")).contains(value))invalid(); }
    private static void invalid() {throw new ExportException(ExportException.Reason.INVALID_FILTER);}
    public java.util.List<String> labels() {
        var l=new java.util.ArrayList<String>();
        Object[][] pairs={{"ID",id},{"Usuário",userId},{"Definição",chargeDefinitionId},{"Ciclo",chargeCycleId},{"Evento",eventId},{"Recorrência",recurrence},{"Ano",year},{"Mês",month},{"Situação",status},{"Método",paymentMethod},{"Situação da cobrança",chargeStatus},{"Situação efetiva",effectiveStatus},{"Vencimento desde",dueFrom},{"Vencimento até",dueTo},{"Período desde",from},{"Período até",to}};
        for(var p:pairs)if(p[1]!=null)l.add(p[0]+": "+ExportValues.text(p[1]));return l;
    }
}
