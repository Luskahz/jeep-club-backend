package com.jeepclub.backend.vehicles.core.application.services;

import com.jeepclub.backend.vehicles.api.dto.list.ListResponseDTO;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListService {

    private final VehicleRepository vehicleRepository;

    public Page<ListResponseDTO> execute(Long memberId, Pageable pageable) {
        return vehicleRepository
                .findAllByOwnerIdAndStatus(memberId, VehicleStatus.ACTIVE, pageable)
                .map(ListResponseDTO::from);
    }

    public Page<ListResponseDTO> executeAsAdmin(Pageable pageable) {
        return vehicleRepository
                .findAllByStatus(VehicleStatus.ACTIVE, pageable)
                .map(ListResponseDTO::from);
    }
}
