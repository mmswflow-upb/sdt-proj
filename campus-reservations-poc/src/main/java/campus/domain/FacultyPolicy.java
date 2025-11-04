package campus.domain;

import java.util.List;
import java.util.Objects;

public class FacultyPolicy {
    private final String facultyKey;
    private final int maxMinutes;
    private final boolean enforceBlackout;
    private final boolean hardPref;
    private final List<Role> allowedRoles;

    public FacultyPolicy(String facultyKey, int maxMinutes, boolean enforceBlackout, boolean hardPref, List<Role> allowedRoles) {
        this.facultyKey = Objects.requireNonNull(facultyKey);
        this.maxMinutes = maxMinutes;
        this.enforceBlackout = enforceBlackout;
        this.hardPref = hardPref;
        this.allowedRoles = List.copyOf(allowedRoles);
    }

    public String getFacultyKey() { return facultyKey; }
    public int getMaxMinutes() { return maxMinutes; }
    public boolean isEnforceBlackout() { return enforceBlackout; }
    public boolean isHardPref() { return hardPref; }
    public List<Role> getAllowedRoles() { return allowedRoles; }
}
