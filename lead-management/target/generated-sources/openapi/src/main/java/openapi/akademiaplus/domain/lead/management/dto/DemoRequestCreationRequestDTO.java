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
 * DemoRequestCreationRequestDTO
 */

@JsonTypeName("DemoRequestCreationRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-07T15:07:48.896065-06:00[America/Mexico_City]", comments = "Generator version: 7.20.0")
public class DemoRequestCreationRequestDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private String firstName;

  private String lastName;

  private String email;

  private String companyName;

  private @Nullable String message;

  public DemoRequestCreationRequestDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DemoRequestCreationRequestDTO(String firstName, String lastName, String email, String companyName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.companyName = companyName;
  }

  public DemoRequestCreationRequestDTO firstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  @NotNull @Size(max = 100) 
  @Schema(name = "first_name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("first_name")
  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public DemoRequestCreationRequestDTO lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  @NotNull @Size(max = 100) 
  @Schema(name = "last_name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("last_name")
  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public DemoRequestCreationRequestDTO email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @NotNull @Size(max = 255) @jakarta.validation.constraints.Email 
  @Schema(name = "email", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public DemoRequestCreationRequestDTO companyName(String companyName) {
    this.companyName = companyName;
    return this;
  }

  /**
   * Get companyName
   * @return companyName
   */
  @NotNull @Size(max = 200) 
  @Schema(name = "company_name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("company_name")
  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public DemoRequestCreationRequestDTO message(@Nullable String message) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DemoRequestCreationRequestDTO demoRequestCreationRequest = (DemoRequestCreationRequestDTO) o;
    return Objects.equals(this.firstName, demoRequestCreationRequest.firstName) &&
        Objects.equals(this.lastName, demoRequestCreationRequest.lastName) &&
        Objects.equals(this.email, demoRequestCreationRequest.email) &&
        Objects.equals(this.companyName, demoRequestCreationRequest.companyName) &&
        Objects.equals(this.message, demoRequestCreationRequest.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(firstName, lastName, email, companyName, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DemoRequestCreationRequestDTO {\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    companyName: ").append(toIndentedString(companyName)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

