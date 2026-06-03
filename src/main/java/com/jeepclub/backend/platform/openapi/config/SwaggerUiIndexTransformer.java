package com.jeepclub.backend.platform.openapi.config;

import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SwaggerUiIndexTransformer implements SwaggerIndexTransformer {

    private static final String CUSTOM_SWAGGER_SCRIPT = """
            <script src="/openapi-custom/swagger-operation-groups.js"></script>
            """;

    private static final String CUSTOM_SWAGGER_STYLE = """
            <link rel="stylesheet" type="text/css" href="/openapi-custom/swagger-operation-groups.css">
            """;

    @Override
    public Resource transform(
            HttpServletRequest request,
            Resource resource,
            ResourceTransformerChain transformerChain
    ) throws IOException {
        Resource transformedResource = transformerChain.transform(request, resource);

        String html = new String(
                transformedResource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        html = html.replace(
                "</head>",
                CUSTOM_SWAGGER_STYLE + "\n</head>"
        );

        html = html.replace(
                "</body>",
                CUSTOM_SWAGGER_SCRIPT + "\n</body>"
        );

        return new TransformedResource(
                transformedResource,
                html.getBytes(StandardCharsets.UTF_8)
        );
    }
}