package io.github.reactivecircus.kstreamlined.backend.store

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.annotation.RegisterReflection

/**
 * Marks a class that Firestore (de)serializes reflectively.
 */
@Target(AnnotationTarget.CLASS)
@RegisterReflection(
    memberCategories = [
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_PUBLIC_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS,
    ],
)
annotation class FirestoreModel
