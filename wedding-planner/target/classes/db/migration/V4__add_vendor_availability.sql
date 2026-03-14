CREATE TABLE vendor_availability (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY DEFAULT (UUID()),
    vendor_id  VARCHAR(36)  NOT NULL,
    date       DATE         NOT NULL,
    reason     VARCHAR(255) NULL,
    CONSTRAINT fk_va_vendor FOREIGN KEY (vendor_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT uq_va_vendor_date UNIQUE (vendor_id, date)
);

CREATE INDEX idx_va_vendor_id ON vendor_availability (vendor_id);
