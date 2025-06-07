package me.zrageyh.telefono.utils;

import java.util.regex.Pattern;

/* 
 * Utility per validazione input utente
 * Previene exploit e garantisce integrità dei dati
 */
public final class ValidationUtils {
    
    private static final Pattern SIM_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZàáâäçéèêëíìîïñóòôöúùûüýÿĀĀĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĴĵĶķĸĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽž ]+$");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}]{1,500}$");
    private static final String[] FORBIDDEN_WORDS = {"admin", "op", "console", "server", "plugin"};
    
    private ValidationUtils() {}
    
    /* Valida numero SIM */
    public static boolean isValidSimNumber(String sim) {
        if (sim == null || sim.trim().isEmpty()) return false;
        String cleaned = sim.trim();
        return SIM_PATTERN.matcher(cleaned).matches() && !cleaned.equals("00000000");
    }
    
    /* Valida nome/cognome contatto */
    public static boolean isValidContactName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String cleaned = name.trim();
        if (cleaned.length() < 2 || cleaned.length() > 15) return false;
        if (!NAME_PATTERN.matcher(cleaned).matches()) return false;
        
        String lower = cleaned.toLowerCase();
        for (String forbidden : FORBIDDEN_WORDS) {
            if (lower.contains(forbidden)) return false;
        }
        return true;
    }
    
    /* Valida messaggio */
    public static boolean isValidMessage(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String cleaned = message.trim();
        if (cleaned.length() > 500) return false;
        return MESSAGE_PATTERN.matcher(cleaned).matches();
    }
    
    /* Sanitizza input rimuovendo caratteri pericolosi */
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.trim()
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("\\s+", " ")
                .substring(0, Math.min(input.length(), 100));
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
} 