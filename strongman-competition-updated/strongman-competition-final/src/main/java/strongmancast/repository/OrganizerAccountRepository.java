package strongmancast.repository;

import strongmancast.model.OrganizerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizerAccountRepository extends JpaRepository<OrganizerAccount, Long> {
    Optional<OrganizerAccount> findByUsername(String username);
}
