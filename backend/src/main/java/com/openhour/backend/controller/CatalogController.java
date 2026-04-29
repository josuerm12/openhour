package com.openhour.backend.controller;

import com.openhour.backend.dto.ServiceOfferingDTO;
import com.openhour.backend.service.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ServiceOfferingDTO> listServices() {
        return catalogService.listServices();
    }
}
