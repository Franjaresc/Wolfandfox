package dev.franjaresc.wolfandfox.route.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Persistable because the id is assigned by the domain. Without it, Spring Data would treat every
 * new closure as an update and run a needless SELECT before the INSERT.
 */
@Entity
@Table(name = "route_closure")
class RouteClosureJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String routeCode;

    @Column(nullable = false)
    private LocalDate closureDate;

    @Column(nullable = false, length = 20)
    private String vehicleCode;

    @Column(nullable = false, length = 20)
    private String driverCode;

    @Column(nullable = false)
    private int packagesDelivered;

    @Column(nullable = false)
    private int packagesFailed;

    @Column(nullable = false)
    private int packagesReturned;

    @Column(nullable = false)
    private int kmDriven;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, length = 100)
    private String closedBy;

    @Column(nullable = false)
    private Instant closedAt;

    @Transient
    private boolean isNew = true;

    protected RouteClosureJpaEntity() {
    }

    RouteClosureJpaEntity(
            UUID id,
            String routeCode,
            LocalDate closureDate,
            String vehicleCode,
            String driverCode,
            int packagesDelivered,
            int packagesFailed,
            int packagesReturned,
            int kmDriven,
            String notes,
            String closedBy,
            Instant closedAt) {
        this.id = id;
        this.routeCode = routeCode;
        this.closureDate = closureDate;
        this.vehicleCode = vehicleCode;
        this.driverCode = driverCode;
        this.packagesDelivered = packagesDelivered;
        this.packagesFailed = packagesFailed;
        this.packagesReturned = packagesReturned;
        this.kmDriven = kmDriven;
        this.notes = notes;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    String getRouteCode() {
        return routeCode;
    }

    LocalDate getClosureDate() {
        return closureDate;
    }

    String getVehicleCode() {
        return vehicleCode;
    }

    String getDriverCode() {
        return driverCode;
    }

    int getPackagesDelivered() {
        return packagesDelivered;
    }

    int getPackagesFailed() {
        return packagesFailed;
    }

    int getPackagesReturned() {
        return packagesReturned;
    }

    int getKmDriven() {
        return kmDriven;
    }

    String getNotes() {
        return notes;
    }

    String getClosedBy() {
        return closedBy;
    }

    Instant getClosedAt() {
        return closedAt;
    }
}
