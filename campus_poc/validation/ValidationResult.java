package campus_poc.validation;
import campus_poc.domain.Room;
public class ValidationResult {
    public final boolean ok;
    public final String msg;
    public final Room chosen;
    private ValidationResult(boolean ok, String msg, Room chosen) { this.ok = ok; this.msg = msg; this.chosen = chosen; }
    public static ValidationResult ok() { return new ValidationResult(true, null, null); }
    public static ValidationResult ok(Room r) { return new ValidationResult(true, null, r); }
    public static ValidationResult fail(String msg) { return new ValidationResult(false, msg, null); }
}
