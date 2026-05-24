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

class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.mk.contractservice");

    private final DescribedPredicate<JavaClass> areStandard = resideInAnyPackage(
            "",
            "java..",
            "javax..",
            "jakarta..",
            "org.slf4j..",
            "org.jspecify..",
            "lombok.."
    );

    /**
     * Documented compromise: {@code Page} and {@code Pageable} from Spring Data are stable,
     * widely adopted pagination abstractions. Replacing them with custom domain types would
     * add complexity without meaningful benefit at this project scale.
     * All other Spring packages are forbidden in the domain layer.
     */
    private final DescribedPredicate<JavaClass> areAllowedSpringInDomain = resideInAnyPackage(
            "org.springframework.data.domain.."
    );

    private final DescribedPredicate<JavaClass> areCore = resideInAnyPackage("com.mk.contractservice.domain..");

    private final DescribedPredicate<JavaClass> areFeatures = resideInAnyPackage("com.mk.contractservice.features..");

    private final DescribedPredicate<JavaClass> areInfrastructure = resideInAnyPackage("com.mk.contractservice.infrastructure..");
    private final DescribedPredicate<JavaClass> areControllers = resideInAnyPackage("com.mk.contractservice.controllers..");

    @Test
    void domain_should_not_depend_on_infrastructure() {
        noClasses()
                .that(areCore)
                .should()
                .dependOnClassesThat(areInfrastructure)
                .because("Domain must remain pure and not depend on infrastructure details (JPA, Spring Data, etc.)")
                .check(classes);
    }

    @Test
    void domain_should_not_depend_on_features() {
        noClasses()
                .that(areCore)
                .should()
                .dependOnClassesThat(areFeatures)
                .because("Domain is the core layer and should not depend on application use cases")
                .check(classes);
    }

    @Test
    void domain_should_not_depend_on_controllers() {
        noClasses()
                .that(areCore)
                .should()
                .dependOnClassesThat(areControllers)
                .because("Domain must not depend on presentation layer (controllers)")
                .check(classes);
    }

    @Test
    void domain_should_only_depend_on_itself_and_standard_classes() {
        classes()
                .that(areCore)
                .should()
                .onlyDependOnClassesThat(areCore.or(areStandard).or(areAllowedSpringInDomain))
                .because("Domain must be framework-agnostic. Only standard Java/Jakarta APIs and " +
                         "Spring Data pagination types (Page, Pageable) are tolerated as a documented compromise.")
                .check(classes);
    }

    @Test
    void features_should_not_depend_on_controllers() {
        noClasses()
                .that(areFeatures)
                .should()
                .dependOnClassesThat(areControllers)
                .because("Features (application layer) must not depend on presentation layer (controllers)")
                .check(classes);
    }
}
