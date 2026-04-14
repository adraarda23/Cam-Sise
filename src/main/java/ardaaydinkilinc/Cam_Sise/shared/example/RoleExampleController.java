package ardaaydinkilinc.Cam_Sise.shared.example;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rol tabanlı yetkilendirme örnekleri
 * Bu controller sadece örnek amaçlıdır, gerçek implementasyonlarda silinebilir
 */
@RestController
@RequestMapping("/api/example")
public class RoleExampleController {

    // Sadece ADMIN rolü erişebilir
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin-only")
    public String adminOnly() {
        return "Bu endpoint sadece ADMIN erişebilir";
    }

    // ADMIN veya COMPANY_STAFF erişebilir
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    @GetMapping("/staff-area")
    public String staffArea() {
        return "ADMIN veya COMPANY_STAFF erişebilir";
    }

    // Sadece CUSTOMER rolü erişebilir
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/customer-only")
    public String customerOnly() {
        return "Bu endpoint sadece CUSTOMER erişebilir";
    }

    // Herhangi bir authenticated kullanıcı erişebilir
    @GetMapping("/authenticated")
    public String authenticated() {
        return "Token varsa herkes erişebilir";
    }
}
