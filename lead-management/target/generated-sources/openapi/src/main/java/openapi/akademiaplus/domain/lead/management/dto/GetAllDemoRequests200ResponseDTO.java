package openapi.akademiaplus.domain.lead.management.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * GetAllDemoRequests200ResponseDTO
 */

@JsonTypeName("GetAllDemoRequests200Response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-07T15:07:48.896065-06:00[America/Mexico_City]", comments = "Generator version: 7.20.0")
public class GetAllDemoRequests200ResponseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Valid
  private List<@Valid GetDemoRequestResponseDTO> demoRequests = new ArrayList<>();

  public GetAllDemoRequests200ResponseDTO demoRequests(List<@Valid GetDemoRequestResponseDTO> demoRequests) {
    this.demoRequests = demoRequests;
    return this;
  }

  public GetAllDemoRequests200ResponseDTO addDemoRequestsItem(GetDemoRequestResponseDTO demoRequestsItem) {
    if (this.demoRequests == null) {
      this.demoRequests = new ArrayList<>();
    }
    this.demoRequests.add(demoRequestsItem);
    return this;
  }

  /**
   * Get demoRequests
   * @return demoRequests
   */
  @Valid 
  @Schema(name = "demo_requests", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("demo_requests")
  public List<@Valid GetDemoRequestResponseDTO> getDemoRequests() {
    return demoRequests;
  }

  public void setDemoRequests(List<@Valid GetDemoRequestResponseDTO> demoRequests) {
    this.demoRequests = demoRequests;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetAllDemoRequests200ResponseDTO getAllDemoRequests200Response = (GetAllDemoRequests200ResponseDTO) o;
    return Objects.equals(this.demoRequests, getAllDemoRequests200Response.demoRequests);
  }

  @Override
  public int hashCode() {
    return Objects.hash(demoRequests);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAllDemoRequests200ResponseDTO {\n");
    sb.append("    demoRequests: ").append(toIndentedString(demoRequests)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

