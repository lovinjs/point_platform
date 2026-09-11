package com.core.coreboot.platform.customer.auth.security;

import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.service.CustomerStoreQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class CustomerSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CustomerStoreQueryService customerStoreQueryService;

    @Test
    void shouldRejectProtectedCustomerEndpointWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/customer/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(30034));
    }

    @Test
    void shouldRejectConsumePinStatusWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/customer/security/consume-pin/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(30034));
    }

    @Test
    void shouldRejectPendingConsumptionQueryWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/customer/consumption-orders/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(30034));
    }

    @Test
    void shouldAllowTicketExchangeEndpointThroughSecurityAndValidateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/h5/auth/session/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10011));
    }

    @Test
    void shouldAllowWechatStartEndpointThroughSecurityAndReportDisabledFeature() throws Exception {
        mockMvc.perform(get("/api/v1/h5/auth/wechat/start"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(30038));
    }

    @Test
    void shouldAllowPublicStoreDirectoryWithoutBearerToken() throws Exception {
        when(customerStoreQueryService.list(1, 20, null)).thenReturn(new PageResult<>(
                1L, 20L, 0L, 0L, false, List.of()
        ));

        mockMvc.perform(get("/api/v1/customer/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void shouldKeepNonGetStoreRequestsProtected() throws Exception {
        mockMvc.perform(post("/api/v1/customer/stores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(30034));
    }
}
