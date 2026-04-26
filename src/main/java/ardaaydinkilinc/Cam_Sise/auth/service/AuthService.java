package ardaaydinkilinc.Cam_Sise.auth.service;

import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.dto.LoginRequest;
import ardaaydinkilinc.Cam_Sise.auth.dto.LoginResponse;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.shared.exception.AuthenticationException;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndActiveTrue(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("Kullanıcı bulunamadı veya aktif değil"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Hatalı şifre");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getPoolOperatorId());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .fillerId(user.getFillerId())
                .build();
    }
}
