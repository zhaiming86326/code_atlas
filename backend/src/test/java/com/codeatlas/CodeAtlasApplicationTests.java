package com.codeatlas;

import com.codeatlas.service.DemoDataService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.context.SpringBootTest(properties = "spring.profiles.active=demo")
class CodeAtlasApplicationTests {
    @Test
    void demoContractContainsDashboardDocumentsAndCitations() {
        var data = new DemoDataService();

        assertThat(data.health().status()).isEqualTo("ok");
        assertThat(data.dashboard().documents()).isPositive();
        assertThat(data.documents()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(data.search(new com.codeatlas.domain.ApiModels.SearchRequest(
                "Spring Boot 配置", java.util.List.of("Spring Boot"), "3.5.x", "HYBRID"
        )).citations()).isNotEmpty();
    }
}
