package ardaaydinkilinc.Cam_Sise.shared.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String SECRET = "test-secret-key-minimum-256-bits-for-HS256-algorithm-security-here";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    @Nested
    @DisplayName("generateToken ve extractClaim")
    class GenerateAndExtract {

        @Test
        @DisplayName("username ve role doğru çıkarılmalı")
        void extractsUsernameAndRole() {
            String token = jwtUtil.generateToken("testuser", "COMPANY_STAFF");
            assertThat(jwtUtil.extractUsername(token)).isEqualTo("testuser");
            assertThat(jwtUtil.extractRole(token)).isEqualTo("COMPANY_STAFF");
        }

        @Test
        @DisplayName("poolOperatorId içeren token üretilmeli")
        void generatesTokenWithPoolOperatorId() {
            String token = jwtUtil.generateToken("admin", "ADMIN", 42L);
            assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin");
            assertThat(jwtUtil.extractPoolOperatorId(token)).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("extractPoolOperatorId")
    class ExtractPoolOperatorId {

        @Test
        @DisplayName("küçük poolOperatorId (Integer olarak saklanır) Long olarak dönmeli")
        void returnsLongForSmallId() {
            String token = jwtUtil.generateToken("user", "COMPANY_STAFF", 1L);
            Long result = jwtUtil.extractPoolOperatorId(token);
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("büyük poolOperatorId (Long olarak saklanır) Long olarak dönmeli")
        void returnsLongForLargeId() {
            long largeId = (long) Integer.MAX_VALUE + 1L;
            String token = jwtUtil.generateToken("user", "ADMIN", largeId);
            Long result = jwtUtil.extractPoolOperatorId(token);
            assertThat(result).isEqualTo(largeId);
        }

        @Test
        @DisplayName("poolOperatorId claim yoksa null dönmeli")
        void returnsNullWhenClaimAbsent() {
            String token = jwtUtil.generateToken("user", "CUSTOMER");
            Long result = jwtUtil.extractPoolOperatorId(token);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("geçerli token ve doğru username → true")
        void returnsTrueForValidTokenAndMatchingUsername() {
            String token = jwtUtil.generateToken("ali", "COMPANY_STAFF");
            assertThat(jwtUtil.validateToken(token, "ali")).isTrue();
        }

        @Test
        @DisplayName("geçerli token ama yanlış username → false")
        void returnsFalseForWrongUsername() {
            String token = jwtUtil.generateToken("ali", "COMPANY_STAFF");
            assertThat(jwtUtil.validateToken(token, "mehmet")).isFalse();
        }
    }
}
