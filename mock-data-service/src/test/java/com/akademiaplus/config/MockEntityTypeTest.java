package com.akademiaplus.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;

import static com.akademiaplus.config.MockEntityType.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockEntityType")
class MockEntityTypeTest {

    @Nested
    @DisplayName("Graph integrity")
    class GraphIntegrity {

        @Test
        @DisplayName("Should have no circular dependencies in the graph")
        void shouldHaveNoCircularDependencies_inTheGraph() {
            // Given
            Set<MockEntityType> visited = EnumSet.noneOf(MockEntityType.class);
            Set<MockEntityType> inStack = EnumSet.noneOf(MockEntityType.class);

            // When & Then — DFS cycle detection for every node
            for (MockEntityType entity : values()) {
                assertThat(hasCycle(entity, visited, inStack))
                        .as("Cycle detected involving %s", entity)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Should reach TENANT transitively from every tenant-scoped loadable entity")
        void shouldReachTenantTransitively_fromEveryTenantScopedLoadableEntity() {
            // Given
            Set<MockEntityType> loadableEntities = EnumSet.noneOf(MockEntityType.class);
            for (MockEntityType entity : values()) {
                if (entity.isLoadable() && entity != TENANT && entity != DEMO_REQUEST) {
                    loadableEntities.add(entity);
                }
            }

            // When & Then
            for (MockEntityType entity : loadableEntities) {
                assertThat(transitiveClosure(entity))
                        .as("%s should transitively depend on TENANT", entity)
                        .contains(TENANT);
            }
        }

        @Test
        @DisplayName("Should reach TUTOR transitively from MINOR_STUDENT")
        void shouldReachTutorTransitively_fromMinorStudent() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(MINOR_STUDENT);

            // Then
            assertThat(closure).contains(TUTOR);
        }

        @Test
        @DisplayName("Should reach MEMBERSHIP and COURSE transitively from PAYMENT_ADULT_STUDENT")
        void shouldReachMembershipAndCourseTransitively_fromPaymentAdultStudent() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(PAYMENT_ADULT_STUDENT);

            // Then
            assertThat(closure).contains(MEMBERSHIP, COURSE, ADULT_STUDENT, TENANT);
        }

        @Test
        @DisplayName("Should reach MEMBERSHIP and COURSE transitively from PAYMENT_TUTOR")
        void shouldReachMembershipAndCourseTransitively_fromPaymentTutor() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(PAYMENT_TUTOR);

            // Then
            assertThat(closure).contains(MEMBERSHIP, COURSE, TUTOR, TENANT);
        }

        @Test
        @DisplayName("Should reach SCHEDULE and COLLABORATOR transitively from COURSE_EVENT")
        void shouldReachScheduleAndCollaboratorTransitively_fromCourseEvent() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(COURSE_EVENT);

            // Then
            assertThat(closure).contains(SCHEDULE, COURSE, COLLABORATOR, TENANT);
        }

        @Test
        @DisplayName("Should reach PAYMENT_ADULT_STUDENT transitively from CARD_PAYMENT_INFO")
        void shouldReachPaymentAdultStudentTransitively_fromCardPaymentInfo() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(CARD_PAYMENT_INFO);

            // Then
            assertThat(closure).contains(PAYMENT_ADULT_STUDENT, MEMBERSHIP_ADULT_STUDENT, TENANT);
        }

        @Test
        @DisplayName("Should reach STORE_TRANSACTION and STORE_PRODUCT transitively from STORE_SALE_ITEM")
        void shouldReachTransactionAndProductTransitively_fromStoreSaleItem() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(STORE_SALE_ITEM);

            // Then
            assertThat(closure).contains(STORE_TRANSACTION, STORE_PRODUCT, TENANT);
        }

        @Test
        @DisplayName("Should reach NOTIFICATION transitively from NOTIFICATION_DELIVERY")
        void shouldReachNotificationTransitively_fromNotificationDelivery() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(NOTIFICATION_DELIVERY);

            // Then
            assertThat(closure).contains(NOTIFICATION, TENANT);
        }

        @Test
        @DisplayName("Should reach EMAIL transitively from EMAIL_RECIPIENT")
        void shouldReachEmailTransitively_fromEmailRecipient() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(EMAIL_RECIPIENT);

            // Then
            assertThat(closure).contains(EMAIL, TENANT);
        }

        @Test
        @DisplayName("Should reach EMAIL transitively from EMAIL_ATTACHMENT")
        void shouldReachEmailTransitively_fromEmailAttachment() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(EMAIL_ATTACHMENT);

            // Then
            assertThat(closure).contains(EMAIL, TENANT);
        }
    }

    @Nested
    @DisplayName("Entity properties")
    class EntityProperties {

        @Test
        @DisplayName("Should mark TENANT as loadable")
        void shouldMarkTenantAsLoadable() {
            assertThat(TENANT.isLoadable()).isTrue();
        }

        @Test
        @DisplayName("Should mark TENANT_SEQUENCE as not loadable")
        void shouldMarkTenantSequenceAsNotLoadable() {
            assertThat(TENANT_SEQUENCE.isLoadable()).isFalse();
        }

        @Test
        @DisplayName("Should mark all enum values as cleanable")
        void shouldMarkAllEnumValuesAsCleanable() {
            for (MockEntityType entity : values()) {
                assertThat(entity.isCleanable())
                        .as("%s should be cleanable", entity)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should have no dependencies for TENANT")
        void shouldHaveNoDependencies_forTenant() {
            assertThat(TENANT.getDependencies()).isEmpty();
        }

        @Test
        @DisplayName("Should have TUTOR as direct dependency for MINOR_STUDENT")
        void shouldHaveTutorAsDirectDependency_forMinorStudent() {
            assertThat(MINOR_STUDENT.getDependencies()).containsExactly(TUTOR);
        }

        @Test
        @DisplayName("Should have COURSE as direct dependency for SCHEDULE")
        void shouldHaveCourseAsDirectDependency_forSchedule() {
            assertThat(SCHEDULE.getDependencies()).containsExactly(COURSE);
        }

        @Test
        @DisplayName("Should have SCHEDULE, COURSE, and COLLABORATOR as direct dependencies for COURSE_EVENT")
        void shouldHaveThreeDirectDependencies_forCourseEvent() {
            assertThat(COURSE_EVENT.getDependencies())
                    .containsExactlyInAnyOrder(SCHEDULE, COURSE, COLLABORATOR);
        }

        @Test
        @DisplayName("Should have MEMBERSHIP, COURSE, and ADULT_STUDENT as direct dependencies for MEMBERSHIP_ADULT_STUDENT")
        void shouldHaveThreeDirectDependencies_forMembershipAdultStudent() {
            assertThat(MEMBERSHIP_ADULT_STUDENT.getDependencies())
                    .containsExactlyInAnyOrder(MEMBERSHIP, COURSE, ADULT_STUDENT);
        }

        @Test
        @DisplayName("Should have MEMBERSHIP, COURSE, and TUTOR as direct dependencies for MEMBERSHIP_TUTOR")
        void shouldHaveThreeDirectDependencies_forMembershipTutor() {
            assertThat(MEMBERSHIP_TUTOR.getDependencies())
                    .containsExactlyInAnyOrder(MEMBERSHIP, COURSE, TUTOR);
        }

        @Test
        @DisplayName("Should have MEMBERSHIP_ADULT_STUDENT as direct dependency for PAYMENT_ADULT_STUDENT")
        void shouldHaveMembershipAdultStudentAsDependency_forPaymentAdultStudent() {
            assertThat(PAYMENT_ADULT_STUDENT.getDependencies())
                    .containsExactly(MEMBERSHIP_ADULT_STUDENT);
        }

        @Test
        @DisplayName("Should have MEMBERSHIP_TUTOR as direct dependency for PAYMENT_TUTOR")
        void shouldHaveMembershipTutorAsDependency_forPaymentTutor() {
            assertThat(PAYMENT_TUTOR.getDependencies())
                    .containsExactly(MEMBERSHIP_TUTOR);
        }

        @Test
        @DisplayName("Should have PAYMENT_ADULT_STUDENT as direct dependency for CARD_PAYMENT_INFO")
        void shouldHavePaymentAdultStudentAsDependency_forCardPaymentInfo() {
            assertThat(CARD_PAYMENT_INFO.getDependencies())
                    .containsExactly(PAYMENT_ADULT_STUDENT);
        }

        @Test
        @DisplayName("Should have STORE_TRANSACTION and STORE_PRODUCT as direct dependencies for STORE_SALE_ITEM")
        void shouldHaveTwoDirectDependencies_forStoreSaleItem() {
            assertThat(STORE_SALE_ITEM.getDependencies())
                    .containsExactlyInAnyOrder(STORE_TRANSACTION, STORE_PRODUCT);
        }

        @Test
        @DisplayName("Should have NOTIFICATION as direct dependency for NOTIFICATION_DELIVERY")
        void shouldHaveNotificationAsDependency_forNotificationDelivery() {
            assertThat(NOTIFICATION_DELIVERY.getDependencies())
                    .containsExactly(NOTIFICATION);
        }

        @Test
        @DisplayName("Should have TENANT as direct dependency for EMAIL")
        void shouldHaveTenantAsDependency_forEmail() {
            assertThat(EMAIL.getDependencies())
                    .containsExactly(TENANT);
        }

        @Test
        @DisplayName("Should have EMAIL as direct dependency for EMAIL_RECIPIENT")
        void shouldHaveEmailAsDependency_forEmailRecipient() {
            assertThat(EMAIL_RECIPIENT.getDependencies())
                    .containsExactly(EMAIL);
        }

        @Test
        @DisplayName("Should have EMAIL as direct dependency for EMAIL_ATTACHMENT")
        void shouldHaveEmailAsDependency_forEmailAttachment() {
            assertThat(EMAIL_ATTACHMENT.getDependencies())
                    .containsExactly(EMAIL);
        }

        @Test
        @DisplayName("Should mark all new domain entities as loadable")
        void shouldMarkAllNewDomainEntitiesAsLoadable() {
            // Given
            MockEntityType[] newEntities = {
                    TENANT_SUBSCRIPTION, TENANT_BILLING_CYCLE, COMPENSATION, MEMBERSHIP,
                    STORE_PRODUCT, STORE_TRANSACTION, NOTIFICATION, COURSE, SCHEDULE,
                    COURSE_EVENT, MEMBERSHIP_ADULT_STUDENT, MEMBERSHIP_TUTOR,
                    PAYMENT_ADULT_STUDENT, PAYMENT_TUTOR,
                    CARD_PAYMENT_INFO, STORE_SALE_ITEM, NOTIFICATION_DELIVERY,
                    EMAIL, EMAIL_RECIPIENT, EMAIL_ATTACHMENT
            };

            // When & Then
            for (MockEntityType entity : newEntities) {
                assertThat(entity.isLoadable())
                        .as("%s should be loadable", entity)
                        .isTrue();
            }
        }
    }

    // ── Helper methods ──

    private boolean hasCycle(MockEntityType node,
                             Set<MockEntityType> visited,
                             Set<MockEntityType> inStack) {
        if (inStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }
        visited.add(node);
        inStack.add(node);
        for (MockEntityType dep : node.getDependencies()) {
            if (hasCycle(dep, visited, inStack)) {
                return true;
            }
        }
        inStack.remove(node);
        return false;
    }

    private Set<MockEntityType> transitiveClosure(MockEntityType start) {
        Set<MockEntityType> closure = EnumSet.noneOf(MockEntityType.class);
        Queue<MockEntityType> queue = new ArrayDeque<>(start.getDependencies());
        while (!queue.isEmpty()) {
            MockEntityType current = queue.poll();
            if (closure.add(current)) {
                queue.addAll(current.getDependencies());
            }
        }
        return closure;
    }
}
