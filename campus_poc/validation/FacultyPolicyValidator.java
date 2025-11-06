package campus_poc.validation;
import campus_poc.domain.*;
import java.time.LocalTime;

public class FacultyPolicyValidator extends ValidationHandler {
    private final FacultyPolicy policy;
    public FacultyPolicyValidator(FacultyPolicy policy) { this.policy = policy; }

    @Override
    protected ValidationResult check(ReservationRequest req, User actor) {
        if (!actor.getFacultyKey().equals(policy.getFacultyKey()))
            return ValidationResult.fail("Wrong faculty for this policy");
        if (!policy.getAllowedRoles().contains(actor.getRole()))
            return ValidationResult.fail("Role not allowed");
        if (req.getSlot().minutes() > policy.getMaxMinutes())
            return ValidationResult.fail("Slot exceeds max minutes");
        if (policy.isEnforceBlackout()) {
            var start = req.getSlot().getStart().toLocalTime();
            var end   = req.getSlot().getEnd().toLocalTime();
            if (start.isBefore(LocalTime.of(8,0)) || end.isAfter(LocalTime.of(20,0)))
                return ValidationResult.fail("Outside permitted hours");
        }
        return ValidationResult.ok();
    }
}
