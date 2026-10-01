package com.codeatlas.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonLimits {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer boundedJsonCustomizer() {
        return builder -> builder.postConfigurer(mapper -> mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxStringLength(2 * 1024 * 1024)
                        .maxDocumentLength(3 * 1024 * 1024).maxNestingDepth(20).build()));
    }
}
