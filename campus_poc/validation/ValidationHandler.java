package campus_poc.validation;
import campus_poc.domain.ReservationRequest;
import campus_poc.domain.User;
public abstract class ValidationHandler {
    private ValidationHandler next;
    public ValidationHandler then(ValidationHandler next) { this.next = next; return next; }
    public ValidationResult validate(ReservationRequest req, User actor) {
        ValidationResult r = check(req, actor);
        if (!r.ok) return r;
        return next == null ? r : next.validate(req, actor);
    }
    protected abstract ValidationResult check(ReservationRequest req, User actor);
}
