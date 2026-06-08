package strongmancast.service;

import jakarta.annotation.PostConstruct;
import strongmancast.model.OrganizerAccount;
import strongmancast.repository.OrganizerAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class OrganizerAccountService implements UserDetailsService {

    private static final Long ORGANIZER_ACCOUNT_ID = 1L;

    private final OrganizerAccountRepository organizerAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultUsername;
    private final String defaultPassword;

    public OrganizerAccountService(
            OrganizerAccountRepository organizerAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${strongmancast.organizer.username:organizer}") String defaultUsername,
            @Value("${strongmancast.organizer.password:strongman}") String defaultPassword
    ) {
        this.organizerAccountRepository = organizerAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    @PostConstruct
    public void initializeOrganizerAccount() {
        if (organizerAccountRepository.existsById(ORGANIZER_ACCOUNT_ID)) {
            return;
        }

        OrganizerAccount account = new OrganizerAccount();
        account.setId(ORGANIZER_ACCOUNT_ID);
        account.setUsername(defaultUsername);
        account.setPasswordHash(passwordEncoder.encode(defaultPassword));
        organizerAccountRepository.save(account);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        OrganizerAccount account = organizerAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Organizer account not found"));

        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles("ORGANIZER")
                .build();
    }

    public OrganizerAccount currentAccount() {
        return organizerAccountRepository.findById(ORGANIZER_ACCOUNT_ID)
                .orElseThrow(() -> new IllegalStateException("Organizer account has not been initialized"));
    }

    public void updateCredentials(String username, String password) {
        OrganizerAccount account = currentAccount();
        account.setUsername(username.trim());

        if (password != null && !password.isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(password));
        }

        organizerAccountRepository.save(account);
    }
}
