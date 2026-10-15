package com.bizengine.system.api;

import com.bizengine.businessuser.repository.BusinessUserRepository;
import com.bizengine.businessuser.repository.ExternalPartnerRepository;
import com.bizengine.config.PasswordConfig;
import com.bizengine.config.SecurityConfig;
import com.bizengine.shared.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SystemStatusController.class, properties = "biz.security.partner-authentication.enabled=false")
@Import({SecurityConfig.class, RequestIdFilter.class, PasswordConfig.class})
class SystemStatusControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    BusinessUserRepository businessUserRepository;

    @MockBean
    ExternalPartnerRepository externalPartnerRepository;

    @Test
    void returnsSystemStatusAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("biz-engine"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().exists("X-Request-ID"));
    }
}
