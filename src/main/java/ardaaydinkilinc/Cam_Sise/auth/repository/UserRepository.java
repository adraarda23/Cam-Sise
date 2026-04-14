package ardaaydinkilinc.Cam_Sise.auth.repository;

import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndActiveTrue(String username);

    boolean existsByUsername(String username);
}