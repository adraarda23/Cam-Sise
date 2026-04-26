package ardaaydinkilinc.Cam_Sise.auth.controller;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.service.UserService;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Kullanıcı yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    private static UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getPoolOperatorId(),
                u.getUsername(),
                u.getFullName(),
                u.getRole().name(),
                u.getFillerId(),
                u.getActive(),
                u.getCreatedAt()
        );
    }

    /**
     * ADMIN creates COMPANY_STAFF for their own tenant.
     */
    @Operation(summary = "Yeni personel oluştur", description = "Admin kendi tenant'ı için COMPANY_STAFF kullanıcısı oluşturur.")
    @ApiResponse(responseCode = "201", description = "Kullanıcı oluşturuldu")
    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createStaff(@RequestBody CreateStaffRequest request, HttpServletRequest httpRequest) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        User user = userService.registerUser(
                poolOperatorId,
                request.username,
                request.password,
                request.fullName,
                Role.COMPANY_STAFF,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    /**
     * COMPANY_STAFF creates CUSTOMER for their own tenant.
     */
    @Operation(summary = "Yeni müşteri oluştur", description = "Personel kendi tenant'ı için CUSTOMER kullanıcısı oluşturur.")
    @ApiResponse(responseCode = "201", description = "Müşteri kullanıcısı oluşturuldu")
    @PostMapping("/customer")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<UserResponse> createCustomer(@RequestBody CreateCustomerRequest request, HttpServletRequest httpRequest) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        User user = userService.registerUser(
                poolOperatorId,
                request.username,
                request.password,
                request.fullName,
                Role.CUSTOMER,
                request.fillerId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    /**
     * ADMIN lists all users for their tenant.
     */
    @Operation(summary = "Tüm kullanıcıları listele")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        String callerRole = jwtUtil.extractRole(httpRequest.getHeader("Authorization").substring(7));

        Role targetRole = "ADMIN".equals(callerRole) ? Role.COMPANY_STAFF : Role.CUSTOMER;
        PageResponse<User> userPage = userService.findByPoolOperatorIdPaged(poolOperatorId, targetRole, search, page, size);

        PageResponse<UserResponse> result = new PageResponse<>(
                userPage.content().stream().map(UserController::toResponse).toList(),
                userPage.totalElements(),
                userPage.totalPages(),
                userPage.number(),
                userPage.size()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Update user name and/or password — same role hierarchy as deactivate.
     */
    @Operation(summary = "Kullanıcı bilgilerini güncelle")
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Kullanıcı ID") @PathVariable Long userId,
            @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        String callerRole = jwtUtil.extractRole(httpRequest.getHeader("Authorization").substring(7));

        User target = userService.findById(userId);
        if (!target.getPoolOperatorId().equals(poolOperatorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean allowed = ("ADMIN".equals(callerRole) && target.getRole() == Role.COMPANY_STAFF)
                || ("COMPANY_STAFF".equals(callerRole) && target.getRole() == Role.CUSTOMER);
        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.updateUser(userId, request.fullName, request.password);
        return ResponseEntity.ok(toResponse(user));
    }

    /**
     * Deactivate a user — ADMIN deactivates staff, COMPANY_STAFF deactivates customers.
     */
    @Operation(summary = "Kullanıcıyı devre dışı bırak")
    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<UserResponse> deactivateUser(
            @Parameter(description = "Kullanıcı ID") @PathVariable Long userId,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        String callerRole = jwtUtil.extractRole(httpRequest.getHeader("Authorization").substring(7));

        // Tenant + role guard: the target user must belong to the same tenant
        // and be a role the caller is allowed to manage
        User target = userService.findById(userId);
        if (!target.getPoolOperatorId().equals(poolOperatorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean allowed = ("ADMIN".equals(callerRole) && target.getRole() == Role.COMPANY_STAFF)
                || ("COMPANY_STAFF".equals(callerRole) && target.getRole() == Role.CUSTOMER);
        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.deactivateUser(userId);
        return ResponseEntity.ok(toResponse(user));
    }

    // ===== Response DTO =====

    public record UserResponse(
            Long id,
            Long poolOperatorId,
            String username,
            String fullName,
            String role,
            Long fillerId,
            Boolean active,
            LocalDateTime createdAt
    ) {}

    // ===== Request DTOs =====

    @Schema(description = "Personel oluşturma request DTO")
    public record CreateStaffRequest(
            @Schema(description = "Kullanıcı adı", example = "staff01", required = true)
            String username,

            @Schema(description = "Şifre", example = "password123", required = true)
            String password,

            @Schema(description = "Ad soyad", example = "Ahmet Yılmaz", required = true)
            String fullName
    ) {}

    @Schema(description = "Müşteri oluşturma request DTO")
    public record CreateCustomerRequest(
            @Schema(description = "Kullanıcı adı", example = "musteri01", required = true)
            String username,

            @Schema(description = "Şifre", example = "password123", required = true)
            String password,

            @Schema(description = "Ad soyad", example = "Veli Çelik", required = true)
            String fullName,

            @Schema(description = "Bağlı dolumcu ID", example = "1")
            Long fillerId
    ) {}

    @Schema(description = "Kullanıcı güncelleme request DTO")
    public record UpdateUserRequest(
            @Schema(description = "Yeni ad soyad")
            String fullName,

            @Schema(description = "Yeni şifre (boş bırakılırsa değişmez)")
            String password
    ) {}
}
