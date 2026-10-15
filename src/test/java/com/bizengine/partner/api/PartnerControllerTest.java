package com.bizengine.partner.api;

import com.bizengine.partner.domain.Partner;
import com.bizengine.partner.domain.PartnerStatus;
import com.bizengine.partner.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean; // import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bizengine.businessuser.repository.BusinessUserRepository;
import com.bizengine.businessuser.repository.ExternalPartnerRepository;
import com.bizengine.config.PasswordConfig;
import com.bizengine.config.SecurityConfig;
import com.bizengine.shared.web.RequestIdFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(value = PartnerController.class, properties = "biz.security.partner-authentication.enabled=false")
@Import({SecurityConfig.class, RequestIdFilter.class, PasswordConfig.class})
class PartnerControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean
    PartnerService service; // Change from @MockitoBean

    @MockBean
    BusinessUserRepository businessUserRepository;

    @MockBean
    ExternalPartnerRepository externalPartnerRepository;

    @Test
    void registersPartnerAndReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();

        // 1. Create a mock instance of the Partner domain model
        Partner mockPartner = org.mockito.Mockito.mock(Partner.class);

        // 2. Define what the getters should return to satisfy your assertions
        when(mockPartner.getId()).thenReturn(id);
        when(mockPartner.getCode()).thenReturn("INTERHUB");
        when(mockPartner.getName()).thenReturn("Interhub");
        when(mockPartner.getStatus()).thenReturn(PartnerStatus.ACTIVE);

        // 3. Stub the service to return this mocked entity
        when(service.register(any(String.class), any(String.class)))
                .thenReturn(mockPartner);


        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"interhub\",\"name\":\"Interhub\"}")) // Fixed escaping {"code":"interhub","name":"Interhub"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("INTERHUB"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsBlankPartnerName() throws Exception {
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"interhub\",\"name\":\" \"}")) // Fixed escaping {"code":"interhub","name":" "}"
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
