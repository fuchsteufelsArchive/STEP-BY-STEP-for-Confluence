package de.fuchsteufels.confluence.contentwizard.data;

public class LicenseInfo {
    private final boolean isValid;
    private final String message;

    public LicenseInfo(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getMessage() {
        return message;
    }
}
