package openapi.akademiaplus.domain.lead.management.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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
 * GetDemoRequestResponseDTO
 */

@JsonTypeName("GetDemoRequestResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-07T15:07:48.896065-06:00[America/Mexico_City]", comments = "Generator version: 7.20.0")
public class GetDemoRequestResponseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private @Nullable Long demoRequestId;

  private @Nullable String firstName;

  private @Nullable String lastName;

  private @Nullable String email;

  private @Nullable String companyName;

  private @Nullable String message;

  private @Nullable String status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdAt;

  public GetDemoRequestResponseDTO demoRequestId(@Nullable Long demoRequestId) {
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

  public GetDemoRequestResponseDTO firstName(@Nullable String firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  
  @Schema(name = "first_name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("first_name")
  public @Nullable String getFirstName() {
    return firstName;
  }

  public void setFirstName(@Nullable String firstName) {
    this.firstName = firstName;
  }

  public GetDemoRequestResponseDTO lastName(@Nullable String lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  
  @Schema(name = "last_name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("last_name")
  public @Nullable String getLastName() {
    return lastName;
  }

  public void setLastName(@Nullable String lastName) {
    this.lastName = lastName;
  }

  public GetDemoRequestResponseDTO email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  
  @Schema(name = "email", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public GetDemoRequestResponseDTO companyName(@Nullable String companyName) {
    this.companyName = companyName;
    return this;
  }

  /**
   * Get companyName
   * @return companyName
   */
  
  @Schema(name = "company_name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("company_name")
  public @Nullable String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(@Nullable String companyName) {
    this.companyName = companyName;
  }

  public GetDemoRequestResponseDTO message(@Nullable String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  
  @Schema(name = "message", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public @Nullable String getMessage() {
    return message;
  }

  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  public GetDemoRequestResponseDTO status(@Nullable String status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable String getStatus() {
    return status;
  }

  public void setStatus(@Nullable String status) {
    this.status = status;
  }

  public GetDemoRequestResponseDTO createdAt(@Nullable OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @Valid 
  @Schema(name = "created_at", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("created_at")
  public @Nullable OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(@Nullable OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetDemoRequestResponseDTO getDemoRequestResponse = (GetDemoRequestResponseDTO) o;
    return Objects.equals(this.demoRequestId, getDemoRequestResponse.demoRequestId) &&
        Objects.equals(this.firstName, getDemoRequestResponse.firstName) &&
        Objects.equals(this.lastName, getDemoRequestResponse.lastName) &&
        Objects.equals(this.email, getDemoRequestResponse.email) &&
        Objects.equals(this.companyName, getDemoRequestResponse.companyName) &&
        Objects.equals(this.message, getDemoRequestResponse.message) &&
        Objects.equals(this.status, getDemoRequestResponse.status) &&
        Objects.equals(this.createdAt, getDemoRequestResponse.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(demoRequestId, firstName, lastName, email, companyName, message, status, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetDemoRequestResponseDTO {\n");
    sb.append("    demoRequestId: ").append(toIndentedString(demoRequestId)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    companyName: ").append(toIndentedString(companyName)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

