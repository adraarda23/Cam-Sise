package ardaaydinkilinc.Cam_Sise.auth.service;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long FILLER_ID = 5L;

    private User staffUser;
    private User customerUser;

    @BeforeEach
    void setUp() {
        staffUser = User.register(POOL_OPERATOR_ID, "staff01", "hashed", "Ali Veli", Role.COMPANY_STAFF, null);
        staffUser.clearDomainEvents();

        customerUser = User.register(POOL_OPERATOR_ID, "customer01", "hashed", "Müşteri Bey", Role.CUSTOMER, FILLER_ID);
        customerUser.clearDomainEvents();

        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_new");
    }

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {

        @Test
        @DisplayName("Yeni staff kullanıcı oluşturulabilmeli")
        void shouldRegisterNewStaffUser() {
            when(userRepository.existsByUsername("newstaff")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.registerUser(POOL_OPERATOR_ID, "newstaff", "sifre", "Yeni Staff", Role.COMPANY_STAFF, null);

            assertThat(result.getUsername()).isEqualTo("newstaff");
            assertThat(result.getRole()).isEqualTo(Role.COMPANY_STAFF);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Var olan username ile kayıt IllegalArgumentException fırlatmalı")
        void shouldThrowWhenUsernameAlreadyExists() {
            when(userRepository.existsByUsername("staff01")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.registerUser(POOL_OPERATOR_ID, "staff01", "sifre", "Ali", Role.COMPANY_STAFF, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Aynı filler için customer yoksa yeni customer oluşturulmalı")
        void shouldRegisterNewCustomerWhenNoExistingOne() {
            when(userRepository.findByFillerIdAndActiveTrue(FILLER_ID)).thenReturn(Optional.empty());
            when(userRepository.existsByUsername("newcustomer")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.registerUser(POOL_OPERATOR_ID, "newcustomer", "sifre", "Yeni Müşteri", Role.CUSTOMER, FILLER_ID);

            assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(result.getFillerId()).isEqualTo(FILLER_ID);
        }

        @Test
        @DisplayName("Aynı filler için aktif customer varsa bilgileri güncellenmeli (upsert)")
        void shouldUpsertCustomerWhenExistingActiveCustomer() {
            when(userRepository.findByFillerIdAndActiveTrue(FILLER_ID)).thenReturn(Optional.of(customerUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.registerUser(POOL_OPERATOR_ID, "customer01", "yenisifre", "Güncel İsim", Role.CUSTOMER, FILLER_ID);

            assertThat(result.getFullName()).isEqualTo("Güncel İsim");
            verify(userRepository).save(customerUser);
        }

        @Test
        @DisplayName("Şifre hashlenmiş olarak kaydedilmeli")
        void shouldEncodePassword() {
            when(userRepository.existsByUsername("encoded_user")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.registerUser(POOL_OPERATOR_ID, "encoded_user", "plaintext", "Test", Role.COMPANY_STAFF, null);

            verify(passwordEncoder).encode("plaintext");
            assertThat(result.getPassword()).isEqualTo("hashed_new");
        }
    }

    @Nested
    @DisplayName("changeUserRole()")
    class ChangeUserRole {

        @Test
        @DisplayName("Kullanıcı rolü değiştirilmeli")
        void shouldChangeRole() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(staffUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.changeUserRole(USER_ID, Role.ADMIN);

            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
            verify(userRepository).save(staffUser);
        }

        @Test
        @DisplayName("Kullanıcı bulunamazsa exception fırlatmalı")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeUserRole(USER_ID, Role.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {

        @Test
        @DisplayName("Şifre hashlenmiş haliyle güncellenmeli")
        void shouldUpdatePasswordWithHash() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(staffUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            service.updatePassword(USER_ID, "yenisifre");

            verify(passwordEncoder).encode("yenisifre");
            verify(userRepository).save(staffUser);
        }

        @Test
        @DisplayName("Kullanıcı bulunamazsa exception fırlatmalı")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePassword(USER_ID, "yenisifre"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("fullName verilirse güncellenmeli")
        void shouldUpdateFullName() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(staffUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.updateUser(USER_ID, "Yeni Ad", null);

            assertThat(result.getFullName()).isEqualTo("Yeni Ad");
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("Şifre verilirse encode edilerek güncellenmeli")
        void shouldUpdatePassword() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(staffUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            service.updateUser(USER_ID, null, "yenisifre");

            verify(passwordEncoder).encode("yenisifre");
        }

        @Test
        @DisplayName("İkisi de verilirse her ikisi de güncellenmeli")
        void shouldUpdateBothWhenProvided() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(staffUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.updateUser(USER_ID, "Yeni Ad", "yenisifre");

            assertThat(result.getFullName()).isEqualTo("Yeni Ad");
            verify(passwordEncoder).encode("yenisifre");
        }

        @Test
        @DisplayName("Kullanıcı bulunamazsa exception fırlatmalı")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateUser(USER_ID, "Ad", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Kullanıcı bulunduğunda döndürmeli")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(staffUser));

            User result = service.findById(USER_ID);

            assertThat(result).isEqualTo(staffUser);
        }

        @Test
        @DisplayName("Kullanıcı bulunamazsa exception fırlatmalı")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("findByUsername()")
    class FindByUsername {

        @Test
        @DisplayName("Kullanıcı bulunduğunda döndürmeli")
        void shouldReturnUserWhenFound() {
            when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(staffUser));

            User result = service.findByUsername("staff01");

            assertThat(result.getUsername()).isEqualTo("staff01");
        }

        @Test
        @DisplayName("Kullanıcı bulunamazsa exception fırlatmalı")
        void shouldThrowWhenNotFound() {
            when(userRepository.findByUsername("yok")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByUsername("yok"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findActiveUserByUsername()")
    class FindActiveUserByUsername {

        @Test
        @DisplayName("Aktif kullanıcı bulunduğunda döndürmeli")
        void shouldReturnActiveUser() {
            when(userRepository.findByUsernameAndActiveTrue("staff01")).thenReturn(Optional.of(staffUser));

            User result = service.findActiveUserByUsername("staff01");

            assertThat(result).isEqualTo(staffUser);
        }

        @Test
        @DisplayName("Aktif kullanıcı bulunamazsa exception fırlatmalı")
        void shouldThrowWhenNotFound() {
            when(userRepository.findByUsernameAndActiveTrue("pasif")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findActiveUserByUsername("pasif"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findByPoolOperatorId()")
    class FindByPoolOperatorId {

        @Test
        @DisplayName("PoolOperator'a ait kullanıcıları döndürmeli")
        void shouldReturnUsersForPoolOperator() {
            when(userRepository.findByPoolOperatorId(POOL_OPERATOR_ID))
                    .thenReturn(List.of(staffUser, customerUser));

            List<User> result = service.findByPoolOperatorId(POOL_OPERATOR_ID);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deactivateUser()")
    class DeactivateUser {

        @Test
        @DisplayName("Kullanıcı deaktif edilmeli ve kaydedilmeli")
        void shouldDeactivateAndSave() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(staffUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.deactivateUser(USER_ID);

            assertThat(result.getActive()).isFalse();
            verify(userRepository).save(staffUser);
        }

        @Test
        @DisplayName("Kullanıcı bulunamazsa exception fırlatmalı")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateUser(USER_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
