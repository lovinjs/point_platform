package com.core.coreboot.platform.deployment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductionSecurityConfigTest.ProbeController.class,
        properties = "logging.config=classpath:log4j2-prod.xml")
@ContextConfiguration(classes = {ProductionSecurityConfig.class, ProductionSecurityConfigTest.ProbeController.class})
@ActiveProfiles("prod")
class ProductionSecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowOnlyGetHealthProbe() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(post("/actuator/health")).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectOtherManagementEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/health/db")).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectLegacyMallAndDocumentationPaths() throws Exception {
        mockMvc.perform(get("/user/login")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/order/list")).andExpect(status().isForbidden());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());
    }

    @RestController
    static class ProbeController {
        @GetMapping("/actuator/health")
        String health() {
            return "{\"status\":\"UP\"}";
        }
    }
}
