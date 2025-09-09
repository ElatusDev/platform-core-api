package com.makani.people.employee;

import com.makani.AbstractPerson;
import com.makani.security.user.InternalAuthDataModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "employee")
public class EmployeeDataModel extends AbstractPerson implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "employee_type", nullable = false, length = 50)
    private String employeeType;

    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "internal_auth_id")
    private InternalAuthDataModel internalAuth;
} 
