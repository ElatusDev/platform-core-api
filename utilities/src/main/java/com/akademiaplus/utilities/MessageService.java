/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities;

import com.akademiaplus.utilities.config.BeanConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class MessageService {

    //      people
    private static final String ENTITY_NOT_FOUND = "entity.not.found";
    private static final String ADULT_STUDENT_ENTITY = "entity.adult.student";
    private static final String EMPLOYEE_ENTITY = "entity.employee";
    private static final String COLLABORATOR_ENTITY = "entity.collaborator";
    private static final String TUTOR_ENTITY = "entity.tutor";
    private static final String MINOR_STUDENT_ENTITY = "entity.minor.student";
    private static final String COURSE_ENTITY = "entity.course";
    private static final String COURSE_EVENT_ENTITY = "entity.course.event";
    private static final String MEMBERSHIP_ENTITY = "entity.membership";
    private static final String MEMBERSHIP_ADULT_STUDENT_ENTITY = "entity.membership.adult.student";
    private static final String MEMBERSHIP_TUTOR_ENTITY = "entity.membership.tutor";
    private static final String PAYMENT_ADULT_STUDENT_ENTITY = "entity.payment.adult.student";
    private static final String PAYMENT_TUTOR_ENTITY = "entity.payment.tutor";
    private static final String COMPENSATION_ENTITY = "entity.compensation";
    private static final String NOTIFICATION_ENTITY = "entity.notification";
    private static final String STORE_PRODUCT_ENTITY = "entity.store.product";
    private static final String STORE_TRANSACTION_ENTITY = "entity.store.transaction";

    private static final String INVALID_DATA_EMAIL_CREATION_REQUEST = "invalid.data.email.creation.request";
    private static final String INVALID_DATA_PHONE_CREATION_REQUEST = "invalid.data.phone.creation.request";
    private static final String ENTITY_DELETE_NOT_ALLOWED = "entity.delete.not.allowed";
    private static final String INVALID_UNKNOWN_DATA_REQUEST = "invalid.unknown.data.request";

    //      security
    private static final String INVALID_LOGIN = "invalid.login";

    //      coordination
    private static final String SCHEDULE_NOT_AVAILABLE = "schedule.not.available";
    private static final String SCHEDULE_NOT_FOUND = "schedule.not.found";
    private static final String COURSE_COLLABORATOR_NOT_FOUND="course.collaborator.not.assignable";

    // Internal server error
    private static final String INTERNAL_ERROR_HIGH_SEVERITY = "internal.error.high.severity";

    private final MessageSource messageSource;
    private final Locale locale;
    private String adultStudent;
    private String collaborator;
    private String employee;
    private String tutor;
    private String minorStudent;
    private String course;
    private String courseEvent;
    private String membership;
    private String membershipAdultStudent;
    private String membershipTutor;
    private String paymentAdultStudent;
    private String paymentTutor;
    private String compensation;
    private String notification;
    private String storeProduct;
    private String storeTransaction;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
        this.locale = Locale.forLanguageTag(BeanConfig.LOCALE_LANGUAGE);
    }

    @PostConstruct
    public void init() {
        adultStudent = messageSource.getMessage(ADULT_STUDENT_ENTITY, null, locale);
        collaborator = messageSource.getMessage(COLLABORATOR_ENTITY, null, locale);
        employee = messageSource.getMessage(EMPLOYEE_ENTITY, null, locale);
        tutor = messageSource.getMessage(TUTOR_ENTITY, null, locale);
        minorStudent = messageSource.getMessage(MINOR_STUDENT_ENTITY, null, locale);
        course = messageSource.getMessage(COURSE_ENTITY, null, locale);
        courseEvent = messageSource.getMessage(COURSE_EVENT_ENTITY, null, locale);
        membership = messageSource.getMessage(MEMBERSHIP_ENTITY, null, locale);
        membershipAdultStudent = messageSource.getMessage(MEMBERSHIP_ADULT_STUDENT_ENTITY, null, locale);
        membershipTutor = messageSource.getMessage(MEMBERSHIP_TUTOR_ENTITY, null, locale);
        paymentAdultStudent = messageSource.getMessage(PAYMENT_ADULT_STUDENT_ENTITY, null, locale);
        paymentTutor = messageSource.getMessage(PAYMENT_TUTOR_ENTITY, null, locale);
        compensation = messageSource.getMessage(COMPENSATION_ENTITY, null, locale);
        notification = messageSource.getMessage(NOTIFICATION_ENTITY, null, locale);
        storeProduct = messageSource.getMessage(STORE_PRODUCT_ENTITY, null, locale);
        storeTransaction = messageSource.getMessage(STORE_TRANSACTION_ENTITY, null, locale);
    }

    public String getAdultStudentNotFound(String id) {
        return getEntityNotFound(adultStudent, id);
    }

    public String getCollaboratorNotFound(String id) {
        return getEntityNotFound(collaborator, id);
    }

    public String getEmployeeNotFound(String id) {
        return getEntityNotFound(employee, id);
    }

    public String getTutorNotFound(String id) {
        return getEntityNotFound(tutor, id);
    }

    public String getMinorStudentNotFound(String id) {
        return getEntityNotFound(minorStudent, id);
    }

    public String getCourseNotFound(String id) {
        return getEntityNotFound(course, id);
    }

    public String getCourseEventNotFound(String id) {
        return getEntityNotFound(courseEvent, id);
    }

    public String getMembershipNotFound(String id) {
        return getEntityNotFound(membership, id);
    }

    public String getMembershipAdultStudentNotFound(String id) {
        return getEntityNotFound(membershipAdultStudent, id);
    }

    public String getMembershipTutorNotFound(String id) {
        return getEntityNotFound(membershipTutor, id);
    }

    public String getPaymentAdultStudentNotFound(String id) {
        return getEntityNotFound(paymentAdultStudent, id);
    }

    public String getPaymentTutorNotFound(String id) {
        return getEntityNotFound(paymentTutor, id);
    }

    public String getCompensationNotFound(String id) {
        return getEntityNotFound(compensation, id);
    }

    public String getNotificationNotFound(String id) {
        return getEntityNotFound(notification, id);
    }

    public String getStoreProductNotFound(String id) {
        return getEntityNotFound(storeProduct, id);
    }

    public String getStoreTransactionNotFound(String id) {
        return getEntityNotFound(storeTransaction, id);
    }

    private String getEntityNotFound(String entityName, String id){
        return messageSource.getMessage(ENTITY_NOT_FOUND,
                new Object[] { entityName, id}, locale);
    }

    public String getInvalidDataEmailCreationRequest() {
        return  messageSource.getMessage(INVALID_DATA_EMAIL_CREATION_REQUEST, null, locale);
    }

    public String getInvalidDataPhoneCreationRequest() {
        return messageSource.getMessage(INVALID_DATA_PHONE_CREATION_REQUEST, null, locale);
    }

    public String getAdultStudentDeleteNotAllowed() {
        return getEntityDeleteNotAllowed(adultStudent);
    }

    public String getCollaboratorDeleteNotAllowed() {
        return getEntityDeleteNotAllowed(collaborator);
    }

    public String getEmployeeDeleteNotAllowed() {
        return getEntityDeleteNotAllowed(employee);
    }

    private String getEntityDeleteNotAllowed(String entityName) {
        return messageSource.getMessage(ENTITY_DELETE_NOT_ALLOWED, new Object[]{entityName}, locale);
    }

    public String getInvalidUnknownDataRequest() {
        return messageSource.getMessage(INVALID_UNKNOWN_DATA_REQUEST, null, locale);
    }

    public String getInvalidLogin() {
        return messageSource.getMessage(INVALID_LOGIN, null, locale);
    }

    public String getScheduleNotAvailable(String conflicting) {
        return messageSource.getMessage(SCHEDULE_NOT_AVAILABLE, new Object[] {conflicting}, locale);
    }

    public String getCourseCollaboratorNotFound(String notFounded) {
        return messageSource.getMessage(COURSE_COLLABORATOR_NOT_FOUND, new Object[]{notFounded}, locale);
    }

    public String getScheduleNotFound(String notFounded) {
        return messageSource.getMessage(SCHEDULE_NOT_FOUND, new Object[]{notFounded}, locale);
    }

    public String getInternalErrorHighSeverity(){
        return messageSource.getMessage(INTERNAL_ERROR_HIGH_SEVERITY, null, locale);
    }
}
