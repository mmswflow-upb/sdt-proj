package campus.app;

import campus.domain.ReservationRequest;
import campus.domain.User;
import campus.services.ReservationService;
import campus.validation.ValidationResult;

public class CreateReservationCommand implements Command {
    private final ReservationRequest req;
    private final User actor;
    public CreateReservationCommand(ReservationRequest req, User actor) { this.req = req; this.actor = actor; }
    @Override public void execute(ReservationService service) {
        ValidationResult r = service.submit(req, actor);
        if (!r.ok) throw new IllegalStateException("Submit failed: " + r.msg);
    }
}
