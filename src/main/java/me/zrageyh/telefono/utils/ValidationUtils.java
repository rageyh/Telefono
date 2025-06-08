package me.zrageyh.telefono.utils;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;

import java.util.regex.Pattern;

/* 
 * Utility per validazione input utente
 * Previene exploit e garantisce integrità dei dati
 */
public final class ValidationUtils {
    
    private static final Pattern SIM_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{8,15}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_àáâäçèéêëìíîïñòóôöùúûüý]{2,15}$");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}]{1,500}$");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(".*(['\";\\\\%_]|--|\\/\\*|\\*\\/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union|script).*", Pattern.CASE_INSENSITIVE);
    
    private static final int MAX_NAME_LENGTH = 15;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MIN_SIM_LENGTH = 8;
    private static final int MAX_SIM_LENGTH = 8;
    
    private ValidationUtils() {}
    
    /* Valida numero SIM */
    public static boolean isValidSimNumber(final String sim) {
        if (sim == null || sim.trim().isEmpty()) {
            return false;
        }
        
        final String cleanSim = sanitizeInput(sim);
        return cleanSim.length() >= MIN_SIM_LENGTH && 
               cleanSim.length() <= MAX_SIM_LENGTH && 
               SIM_PATTERN.matcher(cleanSim).matches() &&
               !containsSqlInjection(cleanSim);
    }
    
    public static boolean isValidPhoneNumber(final String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        final String cleanPhone = sanitizeInput(phone);
        return PHONE_PATTERN.matcher(cleanPhone).matches() &&
               !containsSqlInjection(cleanPhone);
    }
    
    /* Valida nome/cognome contatto */
    public static boolean isValidContactName(final String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        final String cleanName = sanitizeInput(name);
        return cleanName.length() <= MAX_NAME_LENGTH &&
               NAME_PATTERN.matcher(cleanName).matches() &&
               !containsSqlInjection(cleanName) &&
               !containsInappropriateContent(cleanName);
    }
    
    /* Valida messaggio */
    public static boolean isValidMessage(final String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        final String cleanMessage = sanitizeInput(message);
        return cleanMessage.length() <= MAX_MESSAGE_LENGTH &&
               MESSAGE_PATTERN.matcher(cleanMessage).matches() &&
               !containsSqlInjection(cleanMessage) &&
               !containsInappropriateContent(cleanMessage);
    }
    
    /* Sanitizza input rimuovendo caratteri pericolosi */
    public static String sanitizeInput(final String input) {
        if (input == null) {
            return "";
        }
        
        return input.trim()
                   .replaceAll("\\p{Cntrl}", "")
                   .replaceAll("[\r\n\t]", "")
                   .replaceAll("\\s+", " ");
    }
    
    public static String sanitizeForDatabase(final String input) {
        if (input == null) {
            return "";
        }
        
        return sanitizeInput(input)
                .replaceAll("(['\";\\\\%_])", "\\\\$1");
    }
    
    public static boolean containsSqlInjection(final String input) {
        if (input == null) {
            return false;
        }
        
        return SQL_INJECTION_PATTERN.matcher(input.toLowerCase()).matches();
    }
    
    private static boolean containsInappropriateContent(final String input) {
        if (input == null) {
            return false;
        }
        
        final String lowerInput = input.toLowerCase();
        final String[] blockedWords = {"admin", "owner", "op", "console", "system", "root"};
        
        for (final String word : blockedWords) {
            if (lowerInput.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    /* Valida range numerico */
    public static boolean isValidRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    /* Valida UUID format */
    public static boolean isValidUUID(String uuid) {
        if (uuid == null) return false;
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public static boolean hasValidPermissions(final Player player, final String permission) {
        if (player == null) {
            return false;
        }
        
        if (player.isOp()) {
            return true;
        }
        
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }
    
    public static boolean isPlayerInValidState(final Player player) {
        return player != null && 
               player.isOnline() && 
               !player.isDead() && 
               player.getWorld() != null;
    }
    
    public static boolean validateInteger(final String input, final int min, final int max) {
        try {
            final int value = Integer.parseInt(sanitizeInput(input));
            return value >= min && value <= max;
        } catch (final NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean validateInventorySlot(final int slot) {
        return slot >= 0 && slot <= 53;
    }
    
    public static void logSecurityEvent(final Player player, final String event, final String details) {
        if (player != null) {
            Common.log("[SECURITY] Player: " + player.getName() + " | Event: " + event + " | Details: " + details);
        }
    }
} 