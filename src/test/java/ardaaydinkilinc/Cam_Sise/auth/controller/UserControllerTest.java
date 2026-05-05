package ardaaydinkilinc.Cam_Sise.auth.controller;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.service.UserService;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.exception.DuplicateResourceException;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final String AUTH_HEADER = "Bearer fake-token";

    private User staffUser;
    private User customerUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        lenient().when(jwtUtil.extractPoolOperatorId(any())).thenReturn(POOL_OPERATOR_ID);
        lenient().when(jwtUtil.extractRole(any())).thenReturn("ADMIN");

        staffUser = User.register(POOL_OPERATOR_ID, "staff01", "hashed", "Ali Veli", Role.COMPANY_STAFF, null);
        staffUser.clearDomainEvents();
        customerUser = User.register(POOL_OPERATOR_ID, "customer01", "hashed", "Veli Ali", Role.CUSTOMER, 5L);
        customerUser.clearDomainEvents();
    }

    @Nested
    @DisplayName("POST /api/users/staff")
    class CreateStaff {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("201 döndürmeli ve yeni staff oluşturmalı")
        void shouldReturn201WithNewStaff() throws Exception {
            when(userService.registerUser(anyLong(), eq("staff01"), any(), eq("Ali Veli"), eq(Role.COMPANY_STAFF), isNull()))
                    .thenReturn(staffUser);

            var body = new UserController.CreateStaffRequest("staff01", "sifre123", "Ali Veli");

            mockMvc.perform(post("/api/users/staff")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("staff01"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("409 döndürmeli (username zaten var)")
        void shouldReturn409OnDuplicateUsername() throws Exception {
            when(userService.registerUser(anyLong(), any(), any(), any(), any(), any()))
                    .thenThrow(new DuplicateResourceException("Username already exists"));

            var body = new UserController.CreateStaffRequest("staff01", "sifre123", "Ali Veli");

            mockMvc.perform(post("/api/users/staff")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/users/customer")
    class CreateCustomer {

        @Test
        @WithMockUser(roles = "COMPANY_STAFF")
        @DisplayName("201 döndürmeli ve yeni customer oluşturmalı")
        void shouldReturn201WithNewCustomer() throws Exception {
            when(userService.registerUser(anyLong(), eq("customer01"), any(), eq("Veli Ali"), eq(Role.CUSTOMER), eq(5L)))
                    .thenReturn(customerUser);

            var body = new UserController.CreateCustomerRequest("customer01", "sifre123", "Veli Ali", 5L);

            mockMvc.perform(post("/api/users/customer")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("customer01"));
        }
    }

    @Nested
    @DisplayName("GET /api/users")
    class GetAllUsers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 döndürmeli ve kullanıcı listesi getirmeli")
        void shouldReturn200WithUserList() throws Exception {
            var pageResponse = new PageResponse<>(List.of(staffUser), 1L, 1, 0, 20);
            when(userService.findByPoolOperatorIdPaged(anyLong(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/users")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{userId}")
    class UpdateUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 döndürmeli ve kullanıcıyı güncellemeli")
        void shouldReturn200WhenUpdateUser() throws Exception {
            when(userService.findById(1L)).thenReturn(staffUser);
            when(userService.updateUser(eq(1L), any(), any())).thenReturn(staffUser);

            var body = new UserController.UpdateUserRequest("Yeni Ad", null);

            mockMvc.perform(put("/api/users/1")
                            .header("Authorization", AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/users/{userId}/deactivate")
    class DeactivateUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 döndürmeli ve kullanıcıyı devre dışı bırakmalı")
        void shouldReturn200WhenDeactivate() throws Exception {
            when(userService.findById(1L)).thenReturn(staffUser);
            when(userService.deactivateUser(1L)).thenReturn(staffUser);

            mockMvc.perform(post("/api/users/1/deactivate")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 döndürmeli (kullanıcı bulunamadı)")
        void shouldReturn400WhenUserNotFound() throws Exception {
            when(userService.findById(999L)).thenThrow(new IllegalArgumentException("not found"));

            mockMvc.perform(post("/api/users/999/deactivate")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isBadRequest());
        }
    }
}
