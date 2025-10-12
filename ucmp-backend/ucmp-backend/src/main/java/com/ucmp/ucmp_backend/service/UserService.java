package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves all users from the database.
     * @return A list of all User entities.
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by their unique college ID.
     * @param collegeId The unique college ID of the user.
     * @return An Optional containing the User entity if found, otherwise an empty Optional.
     */
    public Optional<User> findUserByCollegeId(String collegeId) {
        return userRepository.findByCollegeId(collegeId);
    }

    /**
     * Retrieves a user by their email.
     * @param email The unique email address of the user.
     * @return An Optional containing the User entity if found, otherwise an empty Optional.
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Creates a new user in the database.
     * @param user The User entity to be saved.
     * @return The saved User entity.
     */
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Updates an existing user's information.
     * @param collegeId The college ID of the user to update.
     * @param updatedUser The User entity containing the new data.
     * @return The updated User entity.
     * @throws ResponseStatusException if the user is not found.
     */
    @Transactional
    public User updateUser(String collegeId, User updatedUser) {
        User existingUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with college ID: " + collegeId));

        // Update fields that are allowed to change
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());

        return userRepository.save(existingUser);
    }

    /**
     * Deletes a user from the database by their college ID.
     * @param collegeId The college ID of the user to delete.
     * @throws ResponseStatusException if the user is not found.
     */
    @Transactional
    public void deleteUser(String collegeId) {
        if (!userRepository.existsByCollegeId(collegeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with college ID: " + collegeId);
        }
        userRepository.deleteByCollegeId(collegeId);
    }
}
