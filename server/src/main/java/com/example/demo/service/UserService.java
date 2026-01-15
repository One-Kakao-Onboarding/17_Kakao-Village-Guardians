package com.example.demo.service;

import com.example.demo.dto.request.UpdateUserRequest;
import com.example.demo.entity.User;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.exception.InvalidImageFormatException;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.LdapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final List<String> SUPPORTED_IMAGE_FORMATS = Arrays.asList("png", "jpeg", "jpg", "gif");
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        String ldap = LdapContext.getLdap();
        if (ldap == null) {
            throw new IllegalStateException("No LDAP found in context");
        }
        return findOrCreateUserByLdap(ldap);
    }

    public User findOrCreateUserByLdap(String ldap) {
        // Normalize LDAP to lowercase for consistency
        String normalizedLdap = ldap.toLowerCase().trim();
        return userRepository.findByLdapIgnoreCase(normalizedLdap)
                .orElseGet(() -> {
                    User newUser = new User(normalizedLdap, normalizedLdap);

                    // 기본 아바타 생성 (사용자 이름의 첫 글자 사용)
                    String avatarName = normalizedLdap.substring(0, 1).toUpperCase();
                    String defaultAvatar = String.format(
                        "https://ui-avatars.com/api/?name=%s&size=200&background=random&color=ffffff&bold=true",
                        avatarName
                    );
                    newUser.setAvatar(defaultAvatar);

                    logger.info("Creating new user with LDAP: {} and default avatar", normalizedLdap);
                    return userRepository.save(newUser);
                });
    }

    public Optional<User> getUserByLdap(String ldap) {
        return userRepository.findByLdapIgnoreCase(ldap.toLowerCase().trim());
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (userDetails.getName() != null) {
            user.setName(userDetails.getName());
        }
        if (userDetails.getAvatar() != null) {
            user.setAvatar(userDetails.getAvatar());
        }

        return userRepository.save(user);
    }

    public User updateCurrentUser(UpdateUserRequest request) {
        User currentUser = getCurrentUser();
        logger.info("Updating user: {} (LDAP: {})", currentUser.getName(), currentUser.getLdap());

        // Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            logger.info("Updating name from '{}' to '{}'", currentUser.getName(), request.getName());
            currentUser.setName(request.getName().trim());
        }

        // Update avatar if provided
        if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
            String avatar = request.getAvatar().trim();
            logger.info("Updating avatar (length: {} characters)", avatar.length());
            currentUser.setAvatar(avatar);
        }

        User updatedUser = userRepository.save(currentUser);
        logger.info("User updated successfully: {}", updatedUser.getLdap());

        return updatedUser;
    }

    private void validateImageFormat(String avatar) {
        // 모든 이미지 형식 허용
        logger.debug("Image format validation skipped - all formats allowed");
    }
}
