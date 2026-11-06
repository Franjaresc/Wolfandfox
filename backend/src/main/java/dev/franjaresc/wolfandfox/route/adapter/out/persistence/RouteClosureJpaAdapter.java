package dev.franjaresc.wolfandfox.route.adapter.out.persistence;

import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.PageResult;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.application.port.out.RouteClosureRepositoryPort;
import dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import java.time.LocalDate;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class RouteClosureJpaAdapter implements RouteClosureRepositoryPort {

    /** Declared in V1__init.sql. */
    static final String UNIQUE_ROUTE_AND_DATE = "uq_route_closure_route_code_closure_date";

    /** Matches ix_route_closure_closure_date_route_code in V1__init.sql. */
    private static final Sort DEFAULT_ORDER =
            Sort.by(Sort.Order.desc("closureDate"), Sort.Order.asc("routeCode"));

    private final RouteClosureJpaRepository repository;

    RouteClosureJpaAdapter(RouteClosureJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByRouteCodeAndClosureDate(RouteCode routeCode, LocalDate closureDate) {
        return repository.existsByRouteCodeAndClosureDate(routeCode.value(), closureDate);
    }

    @Override
    public RouteClosure save(RouteClosure closure) {
        try {
            // Flush now, so a constraint violation surfaces here and not at commit, outside this adapter.
            return RouteClosureJpaMapper.toDomain(repository.saveAndFlush(RouteClosureJpaMapper.toEntity(closure)));
        } catch (DataIntegrityViolationException exception) {
            if (violatesUniqueRouteAndDate(exception)) {
                throw new DuplicateRouteClosureException(closure.routeCode(), closure.closureDate());
            }
            throw exception;
        }
    }

    @Override
    public Optional<RouteClosure> findById(RouteClosureId id) {
        return repository.findById(id.value()).map(RouteClosureJpaMapper::toDomain);
    }

    @Override
    public PageResult<RouteClosure> find(RouteClosureFilter filter, PageQuery query) {
        var pageable = PageRequest.of(query.page(), query.size(), DEFAULT_ORDER);
        var page = repository.findAll(matching(filter), pageable);
        return new PageResult<>(
                page.getContent().stream().map(RouteClosureJpaMapper::toDomain).toList(),
                query,
                page.getTotalElements());
    }

    private static Specification<RouteClosureJpaEntity> matching(RouteClosureFilter filter) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.conjunction();
            if (filter.closureDate() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("closureDate"), filter.closureDate()));
            }
            if (filter.routeCode() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("routeCode"), filter.routeCode().value()));
            }
            return predicate;
        };
    }

    private static boolean violatesUniqueRouteAndDate(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return UNIQUE_ROUTE_AND_DATE.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}
