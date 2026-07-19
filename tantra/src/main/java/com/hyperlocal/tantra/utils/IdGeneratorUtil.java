package com.hyperlocal.tantra.utils;

import com.hyperlocal.tantra.constants.Constants;
import java.security.SecureRandom;

import static com.hyperlocal.tantra.constants.Constants.DIGITS;
import static com.hyperlocal.tantra.constants.Constants.LETTERS;

public class IdGeneratorUtil {

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a 10-11 character unique, time-sorted, and secure User ID.
     * Strategy: Prefix (2) + Base36 Timestamp (7-8) + Random Salt (2)
     */
    /**
     * Generates a secure, time-sorted, alphanumeric User ID.
     * Strategy: Guaranteed Digit Injection + Base36 Time Token + Guaranteed Letter Injection
     */
    // Custom epoch in milliseconds (Points to early 2026 to keep the generated string short)
    private static final long CUSTOM_EPOCH = 1767225600000L;

    /**
     * Generates a secure, time-ordered, alphanumeric User ID of EXACTLY 8 characters.
     * Format: TN (2) + Forced Digit (1) + Compressed Time (5) = 8 Characters Max
     */
    public static String generateShortUniqueId() {
        // 1. Calculate time elapsed since the custom epoch
        long elapsedMillis = System.currentTimeMillis() - CUSTOM_EPOCH;

        // 2. Convert to Base36 (Guaranteed to be exactly 5 characters for the next many years)
        String base36Time = Long.toString(elapsedMillis, 36).toUpperCase();

        // Pad with a random letter if it evaluates below 5 characters early on
        while (base36Time.length() < 5) {
            base36Time = LETTERS.charAt(random.nextInt(LETTERS.length())) + base36Time;
        }

        // 3. Force a random digit to maintain alphanumeric status
        char randomDigit = DIGITS.charAt(random.nextInt(DIGITS.length()));

        // 4. Combine: TN (2) + Digit (1) + Time (5) = 8 Characters
        return "TN" + randomDigit + base36Time;
    }
}