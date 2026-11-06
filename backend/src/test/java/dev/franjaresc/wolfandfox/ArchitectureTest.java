package dev.franjaresc.wolfandfox;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inner layers use allow-lists, so a new framework dependency fails by default. ArchUnit also fails
 * a rule that matches no classes, so a renamed package can't silently disable it.
 */
@AnalyzeClasses(packages = "dev.franjaresc.wolfandfox", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..route.domain..";
    private static final String APPLICATION = "..route.application..";
    private static final String PORTS = "..route.application.port..";
    private static final String OUTPUT_PORTS = "..route.application.port.out..";
    private static final String SERVICES = "..route.application.service..";
    private static final String WEB_ADAPTER = "..route.adapter.in.web..";
    private static final String GENERATED_API = "..route.adapter.in.web.generated..";
    private static final String OUTPUT_ADAPTERS = "..route.adapter.out..";
    private static final String PERSISTENCE_ADAPTER = "..route.adapter.out.persistence..";

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfAndTheJdk = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..");

    @ArchTest
    static final ArchRule portsDependOnlyOnTheApplicationDomainAndTheJdk = classes()
            .that().resideInAPackage(PORTS)
            .should().onlyDependOnClassesThat().resideInAnyPackage(APPLICATION, DOMAIN, "java..");

    /**
     * The one framework exception, limited to services and to this annotation. Propagation and
     * Isolation are listed because they are the types of its default attribute values, which ArchUnit
     * reports as dependencies of every annotated method.
     */
    @ArchTest
    static final ArchRule servicesMayAddOnlySpringTransactional = classes()
            .that().resideInAPackage(SERVICES)
            .should().onlyDependOnClassesThat(resideInAnyPackage(APPLICATION, DOMAIN, "java..")
                    .or(equivalentTo(Transactional.class))
                    .or(equivalentTo(Propagation.class))
                    .or(equivalentTo(Isolation.class)));

    @ArchTest
    static final ArchRule adaptersDoNotDependOnEachOther = slices()
            .matching("dev.franjaresc.wolfandfox.route.adapter.(*).(*)..")
            .should().notDependOnEachOther();

    @ArchTest
    static final ArchRule jpaEntitiesLiveInThePersistenceAdapter = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(PERSISTENCE_ADAPTER);

    @ArchTest
    static final ArchRule jpaEntitiesNeverLeaveThePersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE_ADAPTER)
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule controllersReachDataOnlyThroughInputPorts = noClasses()
            .that().resideInAPackage(WEB_ADAPTER)
            .should().dependOnClassesThat().resideInAnyPackage(
                    OUTPUT_ADAPTERS, OUTPUT_PORTS, SERVICES, "org.springframework.data..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule generatedApiCodeStaysInTheWebAdapter = noClasses()
            .that().resideOutsideOfPackage(WEB_ADAPTER)
            .should().dependOnClassesThat().resideInAPackage(GENERATED_API);
}
