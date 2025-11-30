package com.example.facultyservice.service;

import com.example.facultyservice.client.ReservationClient;
import com.example.facultyservice.client.SchedulingClient;
import com.example.facultyservice.dto.FacultyRoomDto;
import com.example.facultyservice.entity.FacultyRoom;
import com.example.facultyservice.repository.FacultyRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing rooms owned by faculties. Operations on rooms are synchronized
 * with the scheduling-service via the SchedulingClient. When rooms are removed or locked
 * reservations are revoked via the ReservationClient.
 */
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

    /**
     * Creates a new room for a faculty. The room is stored locally and also created in the
     * scheduling-service. Throws an exception if the room already exists locally.
     */
    @Transactional
    public FacultyRoom createRoom(FacultyRoomDto dto) {
        if (roomRepository.existsById(dto.getRoomId())) {
            throw new IllegalStateException("Room with id " + dto.getRoomId() + " already exists");
        }
        // Create local entity
        FacultyRoom room = new FacultyRoom(dto.getRoomId(), dto.getFacultyId(), dto.getCapacity(), dto.getEquipment());
        roomRepository.save(room);
        // Call scheduling-service to create the room remotely
        schedulingClient.createRoom(dto.getRoomId(), dto.getFacultyId(), dto.getCapacity(), dto.getEquipment());
        return room;
    }

    /**
     * Updates an existing room. Both the local record and the remote scheduling-service are updated.
     * Throws an exception if the room does not exist locally.
     */
    @Transactional
    public FacultyRoom updateRoom(String roomId, FacultyRoomDto dto) {
        return roomRepository.findById(roomId)
                .map(room -> {
                    room.setFacultyId(dto.getFacultyId());
                    room.setCapacity(dto.getCapacity());
                    room.setEquipment(dto.getEquipment());
                    FacultyRoom saved = roomRepository.save(room);
                    schedulingClient.updateRoom(roomId, dto.getFacultyId(), dto.getCapacity(), dto.getEquipment());
                    return saved;
                })
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    /**
     * Deletes a room. The local record is removed, the remote scheduling-service is called to delete
     * the room and any schedules are removed. All reservations associated with this room are revoked
     * via the reservation-service.
     */
    @Transactional
    public void deleteRoom(String roomId) {
        FacultyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        // Remove remote schedules and room
        schedulingClient.deleteRoom(roomId);
        // Revoke any reservations associated with this room
        reservationClient.revokeReservationsByRoom(roomId);
        // Delete local record
        roomRepository.delete(room);
    }

    /**
     * Locks a room, preventing new reservations. This calls the scheduling-service to lock the room
     * and revokes all existing reservations for the room. The local record is unaffected since
     * locking is managed remotely.
     */
    @Transactional
    public void lockRoom(String roomId) {
        // Throw if local record not found
        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        schedulingClient.lockRoom(roomId);
        reservationClient.revokeReservationsByRoom(roomId);
    }

    /**
     * Unlocks a previously locked room. Delegates to the scheduling-service. Does not modify
     * reservations locally since unlocking does not require any special handling here.
     */
    @Transactional
    public void unlockRoom(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        schedulingClient.unlockRoom(roomId);
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