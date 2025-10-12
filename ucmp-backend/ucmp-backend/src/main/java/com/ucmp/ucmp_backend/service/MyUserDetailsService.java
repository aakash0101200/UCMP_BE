package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.Role;
import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Service to load user details for authentication, used exclusively by Spring Security.
 * This class translates a user's data from the repository into a UserDetails object
 * that Spring Security can understand.
 */
@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Finds a user by their college ID and returns a UserDetails object.
     * This method is called by Spring Security during the login process.
     * @param collegeId The college ID of the user.
     * @return A UserDetails object for the authenticated user.
     * @throws UsernameNotFoundException if the user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String collegeId) throws UsernameNotFoundException {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with collegeId: " + collegeId));

        return new org.springframework.security.core.userdetails.User(
                user.getCollegeId(),
                user.getPassword(),
                mapRolesToAuthorities(user.getRoles()));
    }

    /**
     * Converts a collection of Role entities into Spring Security's GrantedAuthority objects.
     * @param roles The roles assigned to the user.
     * @return A collection of GrantedAuthority objects.
     */
    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }
}