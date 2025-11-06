package campus_poc.app;
import campus_poc.services.ReservationService;
import campus_poc.domain.User;
public class ApproveReservationCommand implements Command {
    private final String requestId;
    private final User actor;
    public ApproveReservationCommand(String requestId, User actor) { this.requestId = requestId; this.actor = actor; }
    @Override public void execute(ReservationService service) { service.approve(requestId, actor); }
}
