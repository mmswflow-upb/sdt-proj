package com.example.facultyservice.service;
import com.example.facultyservice.client.ReservationClient;
import com.example.facultyservice.client.SchedulingClient;
import com.example.facultyservice.dto.FacultyRoomDto;
import com.example.facultyservice.dto.RoomRequest;
import com.example.facultyservice.entity.FacultyRoom;
import com.example.facultyservice.repository.FacultyRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service
public class FacultyRoomService {
    private final FacultyRoomRepository roomRepository;
    private final SchedulingClient schedulingClient;
    private final ReservationClient reservationClient;
    public FacultyRoomService(FacultyRoomRepository roomRepository,
                              SchedulingClient schedulingClient,
                              ReservationClient reservationClient) {
        this.roomRepository = roomRepository;
        this.schedulingClient = schedulingClient;
        this.reservationClient = reservationClient;
    }
    @Transactional
    public FacultyRoom createRoom(FacultyRoomDto dto) {
        if (roomRepository.existsById(dto.getRoomId())) {
            throw new IllegalStateException("Room with id " + dto.getRoomId() + " already exists");
        }
        FacultyRoom room = new FacultyRoom(dto.getRoomId(), dto.getFacultyId(), dto.getCapacity(), dto.getEquipment());
        roomRepository.save(room);
        RoomRequest roomRequest = new RoomRequest(dto.getRoomId(), dto.getFacultyId(), dto.getCapacity(), dto.getEquipment());
        schedulingClient.createRoom(roomRequest);
        return room;
    }
    @Transactional
    public FacultyRoom updateRoom(String roomId, FacultyRoomDto dto) {
        return roomRepository.findById(roomId)
                .map(room -> {
                    room.setFacultyId(dto.getFacultyId());
                    room.setCapacity(dto.getCapacity());
                    room.setEquipment(dto.getEquipment());
                    FacultyRoom saved = roomRepository.save(room);
                    RoomRequest roomRequest = new RoomRequest(roomId, dto.getFacultyId(), dto.getCapacity(), dto.getEquipment());
                    schedulingClient.updateRoom(roomId, roomRequest);
                    return saved;
                })
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }
    @Transactional
    public void deleteRoom(String roomId) {
        FacultyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        schedulingClient.deleteRoom(roomId);
        reservationClient.revokeReservationsByRoom(roomId);
        roomRepository.delete(room);
    }
    @Transactional(readOnly = true)
    public List<FacultyRoom> getAllRooms() {
        return roomRepository.findAll();
    }
    @Transactional(readOnly = true)
    public Optional<FacultyRoom> getRoom(String roomId) {
        return roomRepository.findById(roomId);
    }
}