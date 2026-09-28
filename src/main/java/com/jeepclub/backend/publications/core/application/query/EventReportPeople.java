package com.jeepclub.backend.publications.core.application.query;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import java.util.*;
/** One actual allocation per person. Unallocated dependents never inherit the member's vehicle. */
public final class EventReportPeople {
    private EventReportPeople() {}
    public record Person(String type,Long personId,String name,String cpf,Long userId,String holder,
                         Long registrationId,String registrationStatus,Long vehicleId,String guestStatus) {}
    public static List<Person> assemble(List<EventRegistration> registrations,List<EventGuestRequest> guests,
            Map<Long,UserDetails> users,Map<Long,DependentsQuery.Details> dependents) {
        var result=new ArrayList<Person>();
        for(var r:registrations) {
            var u=users.get(r.userId());String name=u==null?"Cadastro indisponível":u.name();
            Long vehicle=r.allocations().stream().filter(EventRegistration.Allocation::member).map(EventRegistration.Allocation::vehicleId).findFirst().orElse(null);
            result.add(new Person("MEMBER",r.userId(),name,u==null?"":u.cpf(),r.userId(),name,r.id(),r.status().name(),vehicle,null));
            for(var a:r.allocations())for(Long id:a.dependentIds())result.add(dependent(r,id,a.vehicleId(),name,dependents));
            for(Long id:r.unallocatedDependentIds())result.add(dependent(r,id,null,name,dependents));
        }
        for(var g:guests) {
            var u=users.get(g.requesterUserId());
            result.add(new Person("GUEST",g.id(),g.guestName()==null?"Nome não informado (cadastro legado)":g.guestName(),g.cpf(),g.requesterUserId(),u==null?"Cadastro indisponível":u.name(),null,
                g.status()==EventGuestRequest.Status.APPROVED?"CONFIRMED":g.status().name(),g.status()==EventGuestRequest.Status.APPROVED?g.vehicleId():null,g.status().name()));
        }
        return List.copyOf(result);
    }
    private static Person dependent(EventRegistration r,Long id,Long vehicle,String holder,Map<Long,DependentsQuery.Details> ds) {
        var d=ds.get(id);
        // Stale/corrupt links cannot reveal another household's identity.
        boolean valid=d!=null && d.userId().equals(r.userId());
        return new Person("DEPENDENT",id,valid?d.name():"Cadastro indisponível",valid?d.cpf():"",r.userId(),holder,r.id(),r.status().name(),vehicle,null);
    }
}
