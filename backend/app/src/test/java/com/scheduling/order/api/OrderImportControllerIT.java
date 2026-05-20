package com.scheduling.order.api;

import com.scheduling.order.import_.ImportOrchestratorService;
import com.scheduling.order.import_.ImportRetryService;
import com.scheduling.order.import_.ImportStatus;
import com.scheduling.order.import_.ImportTrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrderImportController MockMvc 통합 — TK-01-1-4 IT.
 *
 * <p>RBAC + ProblemDetail + multipart 흐름 회귀. TK-30-2-2 (Spring Security + @PreAuthorize)
 * 활성 후 deferred 였던 검증.
 *
 * <p>검증 케이스:
 * <ul>
 *   <li>정상 — PLANNER role 3 파일 multipart → 202 + trackingId</li>
 *   <li>RBAC — READ_ONLY role → 403 (ProblemDetail Korean)</li>
 *   <li>인증 미부여 — 401 (ProblemDetail Korean)</li>
 *   <li>4 파일 → 400, 빈 파일 → 400, 확장자 .csv → 400</li>
 *   <li>status 조회 — STK_USER role 허용 + ProblemDetail 404</li>
 *   <li>retry — PLANNER 캐시 hit / 410 Gone</li>
 * </ul>
 */
@SpringBootTest
class OrderImportControllerIT {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired private WebApplicationContext context;

    @MockitoBean private ImportOrchestratorService orchestrator;
    @MockitoBean private ImportRetryService retryService;
    @MockitoBean private ImportTrackingService tracking;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        doNothing().when(orchestrator).processAsync(any(UUID.class), anyList());
    }

    private MockMultipartFile xlsx(String name) {
        return new MockMultipartFile("files", name, XLSX, new byte[]{1, 2, 3, 4});
    }

    // ---------- 정상 흐름 ----------

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("PLANNER → 3 파일 multipart → 202 ACCEPTED + trackingId")
    void planner_uploads_three_files_returns_202() throws Exception {
        mockMvc.perform(multipart("/api/v1/orders/import")
                .file(xlsx("monthly.xlsx"))
                .file(xlsx("weekly.xlsx"))
                .file(xlsx("kd.xlsx")))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.trackingId").exists())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.statusUrl").exists())
            .andExpect(jsonPath("$.filenames[0]").value("monthly.xlsx"));

        verify(orchestrator).processAsync(any(UUID.class), anyList());
    }

    @Test
    @WithMockUser(roles = "IT_OPS")
    @DisplayName("IT_OPS → import endpoint 허용")
    void it_ops_can_import() throws Exception {
        mockMvc.perform(multipart("/api/v1/orders/import").file(xlsx("a.xlsx")))
            .andExpect(status().isAccepted());
    }

    // ---------- RBAC ----------

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY → 403 (ProblemDetail Korean)")
    void readonly_role_forbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/orders/import").file(xlsx("a.xlsx")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("STK_USER → 403 (import 미허용)")
    void stk_user_forbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/orders/import").file(xlsx("a.xlsx")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 미부여 → 401 (ProblemDetail Korean)")
    void unauthenticated_returns_401() throws Exception {
        mockMvc.perform(multipart("/api/v1/orders/import").file(xlsx("a.xlsx")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").exists());
    }

    // ---------- multipart 검증 ----------

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("4 파일 → 400 (max 3)")
    void four_files_rejected() throws Exception {
        mockMvc.perform(multipart("/api/v1/orders/import")
                .file(xlsx("a.xlsx"))
                .file(xlsx("b.xlsx"))
                .file(xlsx("c.xlsx"))
                .file(xlsx("d.xlsx")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("빈 파일 → 400")
    void empty_file_rejected() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("files", "empty.xlsx", XLSX, new byte[0]);
        mockMvc.perform(multipart("/api/v1/orders/import").file(empty))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("확장자 .csv → 400")
    void csv_extension_rejected() throws Exception {
        MockMultipartFile csv = new MockMultipartFile("files", "x.csv", "text/csv", new byte[]{1, 2});
        mockMvc.perform(multipart("/api/v1/orders/import").file(csv))
            .andExpect(status().isBadRequest());
    }

    // ---------- status / retry ----------

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("STK_USER → status 조회 허용 (read-mostly)")
    void stk_user_can_query_status() throws Exception {
        UUID id = UUID.randomUUID();
        ImportStatusResponse resp = new ImportStatusResponse(
            id, ImportStatus.PARSED,
            Instant.parse("2026-05-19T04:50:00Z"),
            Instant.parse("2026-05-19T04:55:00Z"),
            List.of("file.xlsx"),
            Map.of("file.xlsx", "MONTHLY_FORECAST:0.95"),
            null
        );
        when(tracking.get(id)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/orders/import/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackingId").value(id.toString()))
            .andExpect(jsonPath("$.status").value("PARSED"));
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("status 미존재 → 404")
    void status_not_found_returns_404() throws Exception {
        UUID id = UUID.randomUUID();
        when(tracking.get(id)).thenThrow(new NoSuchElementException("Tracking ID not found"));

        mockMvc.perform(get("/api/v1/orders/import/" + id))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("PLANNER → retry 캐시 hit → 202")
    void planner_retry_cache_hit() throws Exception {
        UUID id = UUID.randomUUID();
        when(tracking.loadParsedWorkbooks(eq(id)))
            .thenReturn(List.of(new com.scheduling.order.parser.ParsedWorkbook("a.xlsx", List.of())));

        mockMvc.perform(post("/api/v1/orders/import/" + id + "/retry"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.trackingId").value(id.toString()))
            .andExpect(jsonPath("$.status").value("QUEUED"));

        verify(retryService).retryAsync(id);
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("retry 캐시 만료 → 410 Gone")
    void retry_cache_miss_returns_410() throws Exception {
        UUID id = UUID.randomUUID();
        when(tracking.loadParsedWorkbooks(eq(id)))
            .thenThrow(new NoSuchElementException("ParsedWorkbook 캐시 만료: " + id));

        mockMvc.perform(post("/api/v1/orders/import/" + id + "/retry"))
            .andExpect(status().isGone());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY → retry 403")
    void readonly_retry_forbidden() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/orders/import/" + id + "/retry"))
            .andExpect(status().isForbidden());
    }
}
