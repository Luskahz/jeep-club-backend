package com.jeepclub.backend.memberships.infra.security;
import com.jeepclub.backend.memberships.core.port.MemberImportProofPort;
import com.jeepclub.backend.memberships.core.application.result.MemberImportRow;
import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.security.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Short-lived, process-local proof that the exact source was previewed; it never authorizes by itself. */
@Component
public class MemberImportProof implements MemberImportProofPort {
    private final Clock clock;
    private final byte[] key=new byte[32];
    public MemberImportProof(Clock clock) { this.clock=clock; new SecureRandom().nextBytes(key); }
    public String issue(Long actor,List<MemberImportRow> rows,String reason) {
        String expiry=Long.toString(clock.instant().plusSeconds(900).getEpochSecond());
        return expiry+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(mac(expiry,actor,rows,reason));
    }
    public void verify(String token,Long actor,List<MemberImportRow> rows,String reason) {
        try {
            String[] parts=token.split("\\.",-1);
            if(parts.length!=2 || Long.parseLong(parts[0])<=clock.instant().getEpochSecond()
                    || Long.parseLong(parts[0])>clock.instant().plusSeconds(900).getEpochSecond()
                    || !MessageDigest.isEqual(Base64.getUrlDecoder().decode(parts[1]),mac(parts[0],actor,rows,reason))) throw new IllegalArgumentException();
        } catch(RuntimeException invalid) { throw new MemberRegistryException(MemberRegistryException.Kind.INVALID); }
    }
    private byte[] mac(String expiry,Long actor,List<MemberImportRow> rows,String reason) {
        try {
            var bytes=new ByteArrayOutputStream();var data=new DataOutputStream(bytes);
            data.writeUTF(expiry);data.writeLong(actor);data.writeUTF(reason);data.writeInt(rows.size());
            for(var row:rows) {
                data.writeLong(row.identityUserId());data.writeUTF(row.membershipNumber());data.writeBoolean(row.administrativelyConfirmed());data.writeUTF(row.sourceReference());
            }
            var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(bytes.toByteArray());
        } catch(GeneralSecurityException|IOException impossible) { throw new IllegalStateException(impossible); }
    }
}
