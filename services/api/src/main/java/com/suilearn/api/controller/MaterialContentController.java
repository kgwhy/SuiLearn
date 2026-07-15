package com.suilearn.api.controller;

import com.suilearn.api.dto.DocumentRevisionResponse;
import com.suilearn.api.dto.MaterialReadingResponse;
import com.suilearn.api.material.application.MaterialRevisionQueryService;
import com.suilearn.api.material.application.PrivateMaterialAssetService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
public class MaterialContentController {
    private final PrivateMaterialAssetService originals;
    private final MaterialRevisionQueryService revisions;

    public MaterialContentController(PrivateMaterialAssetService originals, MaterialRevisionQueryService revisions) {
        this.originals = originals;
        this.revisions = revisions;
    }

    @GetMapping("/materials/{materialId}/original")
    ResponseEntity<InputStreamResource> viewOriginal(@PathVariable String materialId) {
        return originalResponse(materialId, "inline");
    }

    @GetMapping("/materials/{materialId}/original/download")
    ResponseEntity<InputStreamResource> downloadOriginal(@PathVariable String materialId) {
        return originalResponse(materialId, "attachment");
    }

    @GetMapping("/materials/{materialId}/reading")
    MaterialReadingResponse reading(@PathVariable String materialId, @RequestParam(required = false) String revisionId) {
        return revisions.reading(materialId, revisionId);
    }

    @GetMapping("/materials/{materialId}/revisions/current")
    DocumentRevisionResponse currentRevision(@PathVariable String materialId) {
        return revisions.currentRevision(materialId);
    }

    @GetMapping("/materials/{materialId}/revisions/{revisionId}")
    DocumentRevisionResponse revision(@PathVariable String materialId, @PathVariable String revisionId) {
        return revisions.revision(materialId, revisionId);
    }

    private ResponseEntity<InputStreamResource> originalResponse(String materialId, String disposition) {
        var original = originals.openOriginal(materialId);
        var filename = original.filename() == null || original.filename().isBlank() ? "original" : original.filename();
        var contentDisposition = ContentDisposition.builder(disposition).filename(filename, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .contentType(mediaType(original.mimeType()))
            .body(new InputStreamResource(original.stream()));
    }

    private MediaType mediaType(String mimeType) {
        try {
            return mimeType == null || mimeType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
