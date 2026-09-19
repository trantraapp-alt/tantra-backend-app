package com.hyperlocal.tantra.modules.auth.service;

import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Thin transactional wrapper used by LastActiveFilter's background writer thread.
 * The @Transactional boundary must live on a Spring bean — the filter itself
 * is not suitable because the background thread has no inherited transaction context.
 */
@Service
public class LastActiveService {

    private final UserRepository userRepository;

    public LastActiveService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves userId from mobile and updates last_active_at.
     * Called from a background daemon thread — transaction is opened here.
     */
    @Transactional
    public void touch(String mobile, LocalDateTime ts) {
        userRepository.findByMobileNumber(mobile)
                .ifPresent(user -> userRepository.updateLastActiveAt(user.getUserId(), ts));
    }
}
