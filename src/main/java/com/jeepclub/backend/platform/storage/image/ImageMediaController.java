package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.StorageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/media/images")
@RequiredArgsConstructor
@Tag(name = "Media - Images")
public class ImageMediaController {
    private final ImageMediaService images;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar imagem", description = "Aceita JPEG, PNG ou WebP até 5 MB; confere extensão, MIME e assinatura. Retorna uma chave estável para associar ao recurso de negócio. Imagens não associadas ou substituídas são retidas para limpeza controlada; a API não apaga objetos que possam ser compartilhados.")
    public ResponseEntity<ImageUploadResponse> upload(@RequestPart("file") MultipartFile file) throws IOException {
        String key = images.store(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        return ResponseEntity.status(201).body(new ImageUploadResponse(key, "/media/images?key=" + key));
    }

    @GetMapping
    @Operation(summary = "Resolver imagem pela chave", description = "Retorna o conteúdo da imagem referenciada por chave estável do storage global. Requer autenticação.")
    public ResponseEntity<byte[]> download(@RequestParam String key) {
        StorageResource resource = images.load(key);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(ImageMediaService.contentType(key)))
                .contentLength(resource.size()).body(resource.content());
    }

    public record ImageUploadResponse(
            @Schema(description = "Chave estável; use-a nos campos de foto dos módulos.", example = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png") String storageKey,
            @Schema(description = "Rota autenticada de resolução para o cliente.") String url
    ) {}
}
