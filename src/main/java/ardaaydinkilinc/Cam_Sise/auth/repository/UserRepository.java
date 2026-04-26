package ardaaydinkilinc.Cam_Sise.auth.repository;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndActiveTrue(String username);

    boolean existsByUsername(String username);

    List<User> findByPoolOperatorId(Long poolOperatorId);

    Optional<User> findByFillerIdAndActiveTrue(Long fillerId);

    Page<User> findByPoolOperatorIdAndRole(Long poolOperatorId, Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.poolOperatorId = :poolOperatorId AND u.role = :role " +
           "AND ('' = :search OR u.username ILIKE CONCAT('%', :search, '%') " +
           "OR u.fullName ILIKE CONCAT('%', :search, '%'))")
    Page<User> findByPoolOperatorIdAndRoleFiltered(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("role") Role role,
            @Param("search") String search,
            Pageable pageable
    );
}