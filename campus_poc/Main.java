package campus_poc;

import campus_poc.app.*;
import campus_poc.domain.*;
import campus_poc.eventing.*;
import campus_poc.infra.*;
import campus_poc.services.*;
import campus_poc.strategy.*;

import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection db = DatabaseFactory.create(DatabaseKind.IN_MEMORY);
        EventBus bus = EventBusFactory.create(EventBusKind.IN_MEMORY, null);
        bus.subscribe(DomainEventType.RESERVATION_REQUESTED, e -> System.out.println("EVENT -> " + e));
        bus.subscribe(DomainEventType.RESERVATION_APPROVED, e -> System.out.println("EVENT -> " + e));

        List<Room> rooms = List.of(
            new Room("A101", 30, List.of("projector")),
            new Room("B202", 25, List.of("whiteboard"))
        );
        FacultyPolicy policy = new FacultyPolicy("ENG", 180, true, true, List.of(Role.PROFESSOR, Role.ADMIN));

        SchedulingService scheduling = new SchedulingService(new LowestConflictStrategy(), rooms);
        ReservationService service = new ReservationService(db, bus, scheduling, policy);

        CommandBus commandBus = CommandBus.getInstance(service);

        User prof = new User("u1", Role.PROFESSOR, "ENG");
        User admin = new User("admin", Role.ADMIN, "ENG");

        var req = new ReservationRequest(
            prof.getUserId(), "A101",
            new TimeSlot(LocalDateTime.now().withHour(10).withMinute(0),
                         LocalDateTime.now().withHour(11).withMinute(30)),
            20, List.of("projector")
        );
        commandBus.dispatch(new CreateReservationCommand(req, prof));
        System.out.println("Created request id = " + req.getRequestId());

        var req2 = new ReservationRequest(
            prof.getUserId(), "A101",
            new TimeSlot(LocalDateTime.now().withHour(10).withMinute(30),
                         LocalDateTime.now().withHour(11).withMinute(45)),
            15, List.of("whiteboard")
        );
        try {
            commandBus.dispatch(new CreateReservationCommand(req2, prof));
            System.out.println("Second request room after validation: " + req2.getRoomId());
        } catch (Exception ex) {
            System.out.println("Second request failed: " + ex.getMessage());
        }

        commandBus.dispatch(new ApproveReservationCommand(req.getRequestId(), admin));
    }
}
