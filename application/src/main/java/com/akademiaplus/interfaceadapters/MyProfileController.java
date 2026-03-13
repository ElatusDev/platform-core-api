/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.usecases.my.*;
import openapi.akademiaplus.domain.my.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for self-service endpoints.
 *
 * <p>All endpoints derive user identity from {@code UserContextHolder}
 * (populated by {@code UserContextLoader} filter from JWT claims).
 * No endpoint accepts a user ID from request parameters.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/my")
public class MyProfileController {

    private final UserContextHolder userContextHolder;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final GetMyCoursesUseCase getMyCoursesUseCase;
    private final GetMyCollaboratorCoursesUseCase getMyCollaboratorCoursesUseCase;
    private final GetMyClassesUseCase getMyClassesUseCase;
    private final GetMyClassStudentsUseCase getMyClassStudentsUseCase;
    private final GetMyScheduleUseCase getMyScheduleUseCase;
    private final GetMyMembershipsUseCase getMyMembershipsUseCase;
    private final GetMyPaymentsUseCase getMyPaymentsUseCase;
    private final GetMyChildrenUseCase getMyChildrenUseCase;
    private final GetMyChildCoursesUseCase getMyChildCoursesUseCase;

    /**
     * Constructs the controller with all use case dependencies.
     *
     * @param userContextHolder              the user context holder
     * @param getMyProfileUseCase            get profile use case
     * @param updateMyProfileUseCase         update profile use case
     * @param getMyCoursesUseCase            get courses use case
     * @param getMyCollaboratorCoursesUseCase get collaborator courses use case
     * @param getMyClassesUseCase            get classes use case
     * @param getMyClassStudentsUseCase      get class students use case
     * @param getMyScheduleUseCase           get schedule use case
     * @param getMyMembershipsUseCase        get memberships use case
     * @param getMyPaymentsUseCase           get payments use case
     * @param getMyChildrenUseCase           get children use case
     * @param getMyChildCoursesUseCase       get child courses use case
     */
    public MyProfileController(UserContextHolder userContextHolder,
                                GetMyProfileUseCase getMyProfileUseCase,
                                UpdateMyProfileUseCase updateMyProfileUseCase,
                                GetMyCoursesUseCase getMyCoursesUseCase,
                                GetMyCollaboratorCoursesUseCase getMyCollaboratorCoursesUseCase,
                                GetMyClassesUseCase getMyClassesUseCase,
                                GetMyClassStudentsUseCase getMyClassStudentsUseCase,
                                GetMyScheduleUseCase getMyScheduleUseCase,
                                GetMyMembershipsUseCase getMyMembershipsUseCase,
                                GetMyPaymentsUseCase getMyPaymentsUseCase,
                                GetMyChildrenUseCase getMyChildrenUseCase,
                                GetMyChildCoursesUseCase getMyChildCoursesUseCase) {
        this.userContextHolder = userContextHolder;
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.getMyCoursesUseCase = getMyCoursesUseCase;
        this.getMyCollaboratorCoursesUseCase = getMyCollaboratorCoursesUseCase;
        this.getMyClassesUseCase = getMyClassesUseCase;
        this.getMyClassStudentsUseCase = getMyClassStudentsUseCase;
        this.getMyScheduleUseCase = getMyScheduleUseCase;
        this.getMyMembershipsUseCase = getMyMembershipsUseCase;
        this.getMyPaymentsUseCase = getMyPaymentsUseCase;
        this.getMyChildrenUseCase = getMyChildrenUseCase;
        this.getMyChildCoursesUseCase = getMyChildCoursesUseCase;
    }

    /**
     * Gets the authenticated customer's profile.
     *
     * @return the profile response
     */
    @GetMapping("/profile")
    public ResponseEntity<MyProfileResponseDTO> getMyProfile() {
        return ResponseEntity.ok(getMyProfileUseCase.execute());
    }

    /**
     * Updates the authenticated customer's profile.
     *
     * @param request the update request
     * @return the updated profile response
     */
    @PutMapping("/profile")
    public ResponseEntity<MyProfileResponseDTO> updateMyProfile(@RequestBody UpdateMyProfileRequestDTO request) {
        return ResponseEntity.ok(updateMyProfileUseCase.execute(request));
    }

    /**
     * Lists courses for the authenticated user.
     *
     * <p>Dispatches to the student or collaborator use case based on profile type.</p>
     *
     * @return list of courses
     */
    @GetMapping("/courses")
    public ResponseEntity<List<MyCourseDTO>> getMyCourses() {
        String profileType = userContextHolder.requireProfileType();
        if (JwtTokenProvider.PROFILE_TYPE_COLLABORATOR.equals(profileType)) {
            return ResponseEntity.ok(getMyCollaboratorCoursesUseCase.execute());
        }
        return ResponseEntity.ok(getMyCoursesUseCase.execute());
    }

    /**
     * Lists classes assigned to the authenticated collaborator.
     *
     * @return list of classes
     */
    @GetMapping("/classes")
    public ResponseEntity<List<MyClassDTO>> getMyClasses() {
        return ResponseEntity.ok(getMyClassesUseCase.execute());
    }

    /**
     * Lists students attending a specific class of the authenticated collaborator.
     *
     * @param classId the class (course event) ID
     * @return list of students
     */
    @GetMapping("/classes/{classId}/students")
    public ResponseEntity<List<MyClassStudentDTO>> getMyClassStudents(@PathVariable Long classId) {
        return ResponseEntity.ok(getMyClassStudentsUseCase.execute(classId));
    }

    /**
     * Gets the class schedule for the authenticated student.
     *
     * @return list of schedule entries
     */
    @GetMapping("/schedule")
    public ResponseEntity<List<MyScheduleDTO>> getMySchedule() {
        return ResponseEntity.ok(getMyScheduleUseCase.execute());
    }

    /**
     * Lists memberships for the authenticated student.
     *
     * @return list of memberships
     */
    @GetMapping("/memberships")
    public ResponseEntity<List<MyMembershipDTO>> getMyMemberships() {
        return ResponseEntity.ok(getMyMembershipsUseCase.execute());
    }

    /**
     * Lists payment history for the authenticated student.
     *
     * @return list of payments
     */
    @GetMapping("/payments")
    public ResponseEntity<List<MyPaymentDTO>> getMyPayments() {
        return ResponseEntity.ok(getMyPaymentsUseCase.execute());
    }

    /**
     * Lists minor students for the authenticated tutor.
     *
     * @return list of children
     */
    @GetMapping("/children")
    public ResponseEntity<List<MyChildDTO>> getMyChildren() {
        return ResponseEntity.ok(getMyChildrenUseCase.execute());
    }

    /**
     * Lists courses for a specific child of the authenticated tutor.
     *
     * @param minorStudentId the minor student ID
     * @return list of courses
     */
    @GetMapping("/children/{minorStudentId}/courses")
    public ResponseEntity<List<MyCourseDTO>> getMyChildCourses(@PathVariable Long minorStudentId) {
        return ResponseEntity.ok(getMyChildCoursesUseCase.execute(minorStudentId));
    }
}
