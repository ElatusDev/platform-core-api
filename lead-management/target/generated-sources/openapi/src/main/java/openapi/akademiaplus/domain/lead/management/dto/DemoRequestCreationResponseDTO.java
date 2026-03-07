package openapi.akademiaplus.domain.lead.management.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * DemoRequestCreationResponseDTO
 */

@JsonTypeName("DemoRequestCreationResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-07T15:07:48.896065-06:00[America/Mexico_City]", comments = "Generator version: 7.20.0")
public class DemoRequestCreationResponseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private @Nullable Long demoRequestId;

  public DemoRequestCreationResponseDTO demoRequestId(@Nullable Long demoRequestId) {
    this.demoRequestId = demoRequestId;
    return this;
  }

  /**
   * Get demoRequestId
   * @return demoRequestId
   */
  
  @Schema(name = "demo_request_id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("demo_request_id")
  public @Nullable Long getDemoRequestId() {
    return demoRequestId;
  }

  public void setDemoRequestId(@Nullable Long demoRequestId) {
    this.demoRequestId = demoRequestId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DemoRequestCreationResponseDTO demoRequestCreationResponse = (DemoRequestCreationResponseDTO) o;
    return Objects.equals(this.demoRequestId, demoRequestCreationResponse.demoRequestId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(demoRequestId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DemoRequestCreationResponseDTO {\n");
    sb.append("    demoRequestId: ").append(toIndentedString(demoRequestId)).append("\n");
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

