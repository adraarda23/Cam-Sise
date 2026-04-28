package ardaaydinkilinc.Cam_Sise.settings.repository;

import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
}
