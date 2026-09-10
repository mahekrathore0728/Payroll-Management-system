package com.payroll.repository;

import com.payroll.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN u.employee e WHERE LOWER(u.username) = LOWER(:login) OR LOWER(u.email) = LOWER(:login) OR LOWER(e.email) = LOWER(:login)")
    List<User> findUsersByUsernameOrEmailIgnoreCase(@Param("login") String login);

    default Optional<User> findByUsernameOrEmailIgnoreCase(String login) {
        if (login == null || login.trim().isEmpty()) {
            return Optional.empty();
        }
        List<User> list = findUsersByUsernameOrEmailIgnoreCase(login.trim());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmployeeId(Long employeeId);

    @Query("SELECT COUNT(u) FROM User u WHERE (u.role = 'ROLE_ADMIN' OR u.role = 'ADMIN') AND u.enabled = true")
    long countActiveAdmins();
}
