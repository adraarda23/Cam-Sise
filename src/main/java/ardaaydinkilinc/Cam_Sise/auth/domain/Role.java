package ardaaydinkilinc.Cam_Sise.auth.domain;

/**
 * Kullanıcı rolleri
 *
 * ADMIN: Kullanıcı CRUD, sistem ayarları, zaiyat oran varsayılanları. Tam yetki.
 * COMPANY_STAFF: Operasyon personeli. Tüm dolumcuları, asset'leri, talepleri görür. Rota oluşturur.
 * CUSTOMER: Dolumcu firmanın kullanıcısı. Sadece kendi stoku ve taleplerini görür. Manuel talep açar.
 */
public enum Role {
    ADMIN,
    COMPANY_STAFF,
    CUSTOMER
}