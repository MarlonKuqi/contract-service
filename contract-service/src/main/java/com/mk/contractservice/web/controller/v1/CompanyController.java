package com.mk.contractservice.web.controller.v1;

import com.mk.contractservice.application.ClientApplicationService;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.client.CreateCompanyRequest;
import com.mk.contractservice.web.dto.mapper.client.ClientCreatetMapper;
import com.mk.contractservice.web.dto.mapper.client.ClientDtoMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Clients - Companies")
@RestController
@RequestMapping("/clients/companies")
public class CompanyController {

    private final ClientApplicationService clientApplicationService;
    private final ClientCreatetMapper companyMapper;
    private final ClientDtoMapper clientDtoMapper;

    public CompanyController(final ClientApplicationService clientApplicationService,
                             final ClientCreatetMapper companyMapper,
                             final ClientDtoMapper clientDtoMapper) {
        this.clientApplicationService = clientApplicationService;
        this.companyMapper = companyMapper;
        this.clientDtoMapper = clientDtoMapper;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createCompany(@Valid @RequestBody final CreateCompanyRequest req) {
        final Company company = companyMapper.toEntity(req);
        final Company created = clientApplicationService.createCompany(company);
        final ClientResponse body = clientDtoMapper.toResponse(created);
        return ResponseEntity
                .created(URI.create("/clients/" + created.getId()))
                .body(body);
    }
}
