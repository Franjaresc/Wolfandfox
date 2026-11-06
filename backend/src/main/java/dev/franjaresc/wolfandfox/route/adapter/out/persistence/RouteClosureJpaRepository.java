package dev.franjaresc.wolfandfox.route.adapter.out.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface RouteClosureJpaRepository
        extends JpaRepository<RouteClosureJpaEntity, UUID>, JpaSpecificationExecutor<RouteClosureJpaEntity> {

    boolean existsByRouteCodeAndClosureDate(String routeCode, LocalDate closureDate);
}
