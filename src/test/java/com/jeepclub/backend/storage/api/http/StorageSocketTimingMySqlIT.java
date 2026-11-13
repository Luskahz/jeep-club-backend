package com.jeepclub.backend.storage.api.http;

import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"storage.media.enabled=true","storage.legacy-backfill.enabled=false","server.address=127.0.0.1"})
@ActiveProfiles("test") @Testcontainers
@EnabledIfSystemProperty(named="storage.dedup.socketTiming",matches="true")
class StorageSocketTimingMySqlIT {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46");
    static final Path ROOT;
    static {try {ROOT=Files.createTempDirectory("storage-http-timing-");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("storage.local.root-directory",() -> ROOT.toString());
        r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");r.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto",() -> "validate");r.add("spring.flyway.enabled",() -> true);
    }
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    final java.net.http.HttpClient client=java.net.http.HttpClient.newHttpClient();
    @Autowired UserRegistration users;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.jeepclub.backend.storage.core.repository.UploadAdmission admission;
    private byte[] png(Random rng) throws Exception {
        var image=new BufferedImage(128,128,BufferedImage.TYPE_INT_RGB);int[] pixels=new int[128*128];
        for(int i=0;i<pixels.length;i++) pixels[i]=rng.nextInt(0x1000000);
        image.setRGB(0,0,128,128,pixels,0,128);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
    private void send(String token,byte[] bytes) throws Exception {
        var out=new ByteArrayOutputStream();
        out.write("--boundary123\r\nContent-Disposition: form-data; name=\"file\"; filename=\"fixture.png\"\r\nContent-Type: image/png\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(bytes);out.write("\r\n--boundary123--\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/storage/media/profile"))
            .header("Authorization","Bearer "+token).header("Content-Type","multipart/form-data; boundary=boundary123")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build();
        var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding());
        org.assertj.core.api.Assertions.assertThat(response.statusCode()).isEqualTo(201);
    }
    @Test
    void diagnosticHttpTimingForAnotherUsersPreviouslyStoredImage() throws Exception {
        Object target=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(admission);
        org.assertj.core.api.Assertions.assertThat(org.springframework.test.util.ReflectionTestUtils.getField(target,"hourly")).isEqualTo(30);
        var victim=users.registerAndAuthenticate(new UserRegistrationData("Synthetic victim",null,"timing-victim@example.com","11144477735",null,null,null,Instant.now()),"Password@123");
        var actor=users.registerAndAuthenticate(new UserRegistrationData("Synthetic actor",null,"timing-actor@example.com","93541134780",null,null,null,Instant.now()),"Password@123");
        var rng=new Random(829);var existing=new ArrayList<byte[]>();var fresh=new ArrayList<byte[]>();
        for(int i=0;i<12;i++){byte[] a=png(rng),b=png(rng);existing.add(a);fresh.add(b);send(victim.accessToken(),a);}
        for(int i=0;i<4;i++) send(actor.accessToken(),png(rng));
        record Request(boolean reused,byte[] data) {}
        var requests=new ArrayList<Request>();for(int i=0;i<12;i++){requests.add(new Request(true,existing.get(i)));requests.add(new Request(false,fresh.get(i)));}
        Collections.shuffle(requests,new Random(1234));var newTimes=new ArrayList<Long>();var reuseTimes=new ArrayList<Long>();
        long previous=jdbc.queryForObject("select count(*) from storage_blobs",Long.class);
        for(var request:requests){
            long start=System.nanoTime();send(actor.accessToken(),request.data());long ms=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
            long current=jdbc.queryForObject("select count(*) from storage_blobs",Long.class);
            if(request.reused()) org.assertj.core.api.Assertions.assertThat(current).isEqualTo(previous);
            else org.assertj.core.api.Assertions.assertThat(current).isGreaterThan(previous);
            previous=current;(request.reused()?reuseTimes:newTimes).add(ms);
        }
        Collections.sort(newTimes);Collections.sort(reuseTimes);
        System.out.println("SOCKET_TIMING_DIAGNOSTIC n=12 pixels=128x128 warmup=4 order=randomized mysql=8.0.46 default_attempt_limit=true new_median_ms="+newTimes.get(5)+" new_p95_ms="+newTimes.get(11)+" reused_median_ms="+reuseTimes.get(5)+" reused_p95_ms="+reuseTimes.get(11));
    }
}
