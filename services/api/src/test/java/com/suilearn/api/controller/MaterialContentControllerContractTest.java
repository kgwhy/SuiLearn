package com.suilearn.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.application.MaterialRevisionQueryService;
import com.suilearn.api.material.application.PrivateMaterialAssetService;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class MaterialContentControllerContractTest {
    @Test
    void proxiesThePrivateOriginalWithTheRequestedDispositionWithoutLeakingObjectKey() throws Exception {
        var originals = mock(PrivateMaterialAssetService.class);
        when(originals.openOriginal("mat_1")).thenReturn(new PrivateMaterialAssetService.PrivateOriginal(
            new ByteArrayInputStream("private file".getBytes()), "notes.pdf", "application/pdf"
        ));
        var controller = new MaterialContentController(originals, mock(MaterialRevisionQueryService.class));

        var response = controller.downloadOriginal("mat_1");

        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment", "notes.pdf").doesNotContain("private/object");
        assertThat(response.getBody().getInputStream().readAllBytes()).isEqualTo("private file".getBytes());
    }

    @Test
    void exposesPrivateOriginalReadingAndImmutableRevisionEndpointsDeclaredByOpenApi() {
        var controller = controllerClass();

        assertThat(controller).isPresent();
        var mappings = Arrays.stream(controller.orElseThrow().getDeclaredMethods())
            .map(MaterialContentControllerContractTest::getMapping)
            .flatMap(Optional::stream)
            .flatMap(mapping -> Arrays.stream(mapping.value()))
            .toList();

        assertThat(mappings).contains(
            "/materials/{materialId}/original",
            "/materials/{materialId}/original/download",
            "/materials/{materialId}/reading",
            "/materials/{materialId}/revisions/current",
            "/materials/{materialId}/revisions/{revisionId}"
        );
    }

    private static Optional<Class<?>> controllerClass() {
        try {
            return Optional.of(Class.forName("com.suilearn.api.controller.MaterialContentController"));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<GetMapping> getMapping(Method method) {
        return Optional.ofNullable(method.getAnnotation(GetMapping.class));
    }
}
