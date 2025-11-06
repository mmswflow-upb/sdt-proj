package campus_poc.domain;

import java.util.Objects;

public class User {
    private final String userId;
    private final Role role;
    private final String facultyKey;

    public User(String userId, Role role, String facultyKey) {
        this.userId = Objects.requireNonNull(userId);
        this.role = Objects.requireNonNull(role);
        this.facultyKey = Objects.requireNonNull(facultyKey);
    }

    public String getUserId() { return userId; }
    public Role getRole() { return role; }
    public String getFacultyKey() { return facultyKey; }

    @Override public String toString() { return "User{" + userId + ", " + role + "}"; }
}
