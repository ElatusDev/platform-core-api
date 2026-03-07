package openapi.akademiaplus.domain.lead.management.api;

import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationRequestDTO;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationResponseDTO;
import openapi.akademiaplus.domain.lead.management.dto.ErrorResponseDTO;
import openapi.akademiaplus.domain.lead.management.dto.GetAllDemoRequests200ResponseDTO;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.NativeWebRequest;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-07T15:07:48.896065-06:00[America/Mexico_City]", comments = "Generator version: 7.20.0")
@Controller
@RequestMapping("${openapi.platformCoreAPILeadManagementModule.base-path:}")
public class LeadManagementApiController implements LeadManagementApi {

    private final NativeWebRequest request;

    @Autowired
    public LeadManagementApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

}
