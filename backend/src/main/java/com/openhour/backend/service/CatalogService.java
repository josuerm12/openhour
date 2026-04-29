package com.openhour.backend.service;

import com.openhour.backend.dto.ServiceOfferingDTO;
import com.openhour.backend.exception.BadRequestException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final List<ServiceOfferingDTO> services = List.of(
            new ServiceOfferingDTO("Silk Press", 7500, 2500),
            new ServiceOfferingDTO("Knotless Braids", 18000, 4500),
            new ServiceOfferingDTO("Loc Retwist", 9500, 3000),
            new ServiceOfferingDTO("Wash and Style", 6500, 2000),
            new ServiceOfferingDTO("Color Consultation", 4000, 1500)
    );

    public List<ServiceOfferingDTO> listServices() {
        return services;
    }

    public ServiceOfferingDTO requireService(String name) {
        return services.stream()
                .filter(service -> service.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown service: " + name));
    }
}
