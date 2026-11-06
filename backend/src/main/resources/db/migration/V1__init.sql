CREATE TABLE route_closure (
    id                 uuid          PRIMARY KEY,
    route_code         varchar(20)   NOT NULL,
    closure_date       date          NOT NULL,
    vehicle_code       varchar(20)   NOT NULL,
    driver_code        varchar(20)   NOT NULL,
    packages_delivered integer       NOT NULL,
    packages_failed    integer       NOT NULL,
    packages_returned  integer       NOT NULL,
    km_driven          integer       NOT NULL,
    notes              varchar(1000),
    closed_by          varchar(100)  NOT NULL,
    closed_at          timestamptz   NOT NULL,

    CONSTRAINT uq_route_closure_route_code_closure_date UNIQUE (route_code, closure_date),
    CONSTRAINT ck_route_closure_packages_delivered CHECK (packages_delivered >= 0),
    CONSTRAINT ck_route_closure_packages_failed CHECK (packages_failed >= 0),
    CONSTRAINT ck_route_closure_packages_returned CHECK (packages_returned >= 0),
    CONSTRAINT ck_route_closure_km_driven CHECK (km_driven >= 0)
);

-- Serves the default list order. The unique constraint already covers lookups by route code.
CREATE INDEX ix_route_closure_closure_date_route_code
    ON route_closure (closure_date DESC, route_code ASC);
