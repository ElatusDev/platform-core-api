/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorController;
import com.akademiaplus.currentuser.interfaceadapters.CurrentUserController;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentController;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentController;
import com.akademiaplus.customer.tutor.interfaceadapters.TutorController;
import com.akademiaplus.employee.interfaceadapters.EmployeeController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(basePackageClasses = {EmployeeController.class, CollaboratorController.class,
        AdultStudentController.class, TutorController.class, MinorStudentController.class,
        CurrentUserController.class})
public class PeopleControllerAdvice extends BaseControllerAdvice {

    public PeopleControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
