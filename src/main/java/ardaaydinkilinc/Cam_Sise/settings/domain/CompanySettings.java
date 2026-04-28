package ardaaydinkilinc.Cam_Sise.settings.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "company_settings")
@Getter
@Setter
@NoArgsConstructor
public class CompanySettings {

    @Id
    private Long poolOperatorId;

    private int minPalletRequestQty = 20;

    private int minSeparatorRequestQty = 10;

    public CompanySettings(Long poolOperatorId) {
        this.poolOperatorId = poolOperatorId;
    }

    public int getMinQty(AssetType type) {
        return type == AssetType.PALLET ? minPalletRequestQty : minSeparatorRequestQty;
    }
}
