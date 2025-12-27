package com.mk.contractservice;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.mk.contractservice");

    private final DescribedPredicate<JavaClass> areStandard = resideInAnyPackage(
            "",
            "java..",
            "javax..",
            "jakarta..",  // Jakarta EE (Spring Boot 3+)
            "org.slf4j..",
            "org.jspecify..",  // JSpecify annotations
            "lombok..",
            "org.springframework.."  // Spring Framework (pragmatic choice)
    );

    private final DescribedPredicate<JavaClass> areCore = resideInAnyPackage("com.mk.contractservice.domain..");

    private final DescribedPredicate<JavaClass> areApplication = resideInAnyPackage("com.mk.contractservice.application..");

    private final DescribedPredicate<JavaClass> areInfrastructure = resideInAnyPackage("com.mk.contractservice.infrastructure..");

    @Test
    void domain_should_not_depend_on_infrastructure() {
        noClasses()
                .that(areCore)
                .should()
                .dependOnClassesThat(areInfrastructure)
                .check(classes);
    }

    @Test
    void domain_should_not_depend_on_application() {
        noClasses()
                .that(areCore)
                .should()
                .dependOnClassesThat(areApplication)
                .check(classes);
    }

    @Test
    void domain_should_only_depend_on_itself_and_standard_classes() {
        classes()
                .that(areCore)
                .should()
                .onlyDependOnClassesThat(areCore.or(areStandard))
                .check(classes);
    }

    @Test
    void application_should_not_depend_on_infrastructure() {
        noClasses()
                .that(areApplication)
                .should()
                .dependOnClassesThat(areInfrastructure)
                .check(classes);
    }

    @Test
    void feature_core_should_not_depend_on_feature_web() {
        noClasses()
                .that().resideInAPackage("..feature..core..")
                .should()
                .dependOnClassesThat().resideInAPackage("..feature..web..")
                .because("Core business logic (use cases) must not depend on web adapters (controllers). " +
                        "This ensures the use cases remain framework-agnostic and testable.")
                .check(classes);
    }
}
