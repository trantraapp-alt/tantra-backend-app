package com.hyperlocal.tantra.constants;

public class MessageConstants {

    // English Messages
    public static final String SIGNUP_SUCCESS_EN = "Registration successful! UserID: %s";
    public static final String SIGNUP_ERROR_MOBILE_EN = "Error: Mobile number already registered!";
    public static final String LOGIN_SUCCESS_EN = "Login successful!";
    public static final String LOGIN_ERROR_MOBILE_EN = "Error: Mobile number not registered!";
    public static final String LOGIN_ERROR_PASSWORD_EN = "Error: Invalid password!";
    public static final String OTP_SENT_SUCCESS_EN = "OTP sent successfully. (Check Terminal)";
    public static final String OTP_ERROR_NOT_FOUND_EN = "Error: Mobile number not found in our records.";
    public static final String OTP_ERROR_EXPIRED_EN = "Error: OTP has expired!";
    public static final String OTP_ERROR_INVALID_EN = "Error: Invalid OTP!";
    public static final String PWD_RESET_SUCCESS_EN = "Password reset successful!";
    public static final String PROFILE_ERROR_NOT_FOUND_EN = "Error: User profile not found.";
    public static final String AUTH_ERROR_TOKEN_EN = "Error: Invalid or expired session token!";

    // Hindi Messages
    public static final String SIGNUP_SUCCESS_HI = "पंजीकरण सफल! यूजर आईडी: %s";
    public static final String SIGNUP_ERROR_MOBILE_HI = "त्रुटि: यह मोबाइल नंबर पहले से पंजीकृत है!";
    public static final String LOGIN_SUCCESS_HI = "लॉगिन सफल!";
    public static final String LOGIN_ERROR_MOBILE_HI = "त्रुटि: मोबाइल नंबर पंजीकृत नहीं है!";
    public static final String LOGIN_ERROR_PASSWORD_HI = "त्रुटि: गलत पासवर्ड!";
    public static final String OTP_SENT_SUCCESS_HI = "ओटीपी भेज दिया गया है। (जांचें टर्मिनल)";
    public static final String OTP_ERROR_NOT_FOUND_HI = "त्रुटि: नंबर रिकॉर्ड में नहीं है।";
    public static final String OTP_ERROR_EXPIRED_HI = "त्रुटि: ओटीपी की समय सीमा समाप्त हो गई है!";
    public static final String OTP_ERROR_INVALID_HI = "त्रुटि: गलत ओटीपी!";
    public static final String PWD_RESET_SUCCESS_HI = "पासवर्ड सफलतापूर्वक बदल गया है!";
    public static final String PROFILE_ERROR_NOT_FOUND_HI = "त्रुटि: यूजर प्रोफाइल नहीं मिला।";
    public static final String AUTH_ERROR_TOKEN_HI = "त्रुटि: अवैध या समाप्त हो चुका सत्र (Session) टोकन!";

    /**
     * Resolves localized message string based on the language key preference.
     */
    public static String getMessage(String key, boolean isHindi, Object... args) {
        switch (key) {
            case "SIGNUP_SUCCESS": return isHindi ? String.format(SIGNUP_SUCCESS_HI, args) : String.format(SIGNUP_SUCCESS_EN, args);
            case "SIGNUP_ERROR_MOBILE": return isHindi ? SIGNUP_ERROR_MOBILE_HI : SIGNUP_ERROR_MOBILE_EN;
            case "LOGIN_SUCCESS": return isHindi ? LOGIN_SUCCESS_HI : LOGIN_SUCCESS_EN;
            case "LOGIN_ERROR_MOBILE": return isHindi ? LOGIN_ERROR_MOBILE_HI : LOGIN_ERROR_MOBILE_EN;
            case "LOGIN_ERROR_PASSWORD": return isHindi ? LOGIN_ERROR_PASSWORD_HI : LOGIN_ERROR_PASSWORD_EN;
            case "OTP_SENT_SUCCESS": return isHindi ? OTP_SENT_SUCCESS_HI : OTP_SENT_SUCCESS_EN;
            case "OTP_ERROR_NOT_FOUND": return isHindi ? OTP_ERROR_NOT_FOUND_HI : OTP_ERROR_NOT_FOUND_EN;
            case "OTP_ERROR_EXPIRED": return isHindi ? OTP_ERROR_EXPIRED_HI : OTP_ERROR_EXPIRED_EN;
            case "OTP_ERROR_INVALID": return isHindi ? OTP_ERROR_INVALID_HI : OTP_ERROR_INVALID_EN;
            case "PWD_RESET_SUCCESS": return isHindi ? PWD_RESET_SUCCESS_HI : PWD_RESET_SUCCESS_EN;
            case "PROFILE_ERROR_NOT_FOUND": return isHindi ? PROFILE_ERROR_NOT_FOUND_HI : PROFILE_ERROR_NOT_FOUND_EN;
            case "AUTH_ERROR_TOKEN": return isHindi ? AUTH_ERROR_TOKEN_HI : AUTH_ERROR_TOKEN_EN;
            default: return "";
        }
    }
}