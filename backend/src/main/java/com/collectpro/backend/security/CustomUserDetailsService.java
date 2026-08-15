package com.collectpro.backend.security;

import com.collectpro.backend.entity.User;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
        // Le rôle est chargé en LAZY ; on force son initialisation ici, pendant
        // que la session Hibernate est encore ouverte (grâce à @Transactional).
        // JwtAuthenticationFilter s'exécute avant l'ouverture de session liée à
        // la vue (open-in-view), donc sans ça, getAuthorities() plantait avec
        // une LazyInitializationException.
        Hibernate.initialize(user.getRole());
        return new CustomUserDetails(user);
    }
}