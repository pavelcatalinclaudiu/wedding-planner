CREATE TABLE couple_documents (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    couple_id   CHAR(36)     NOT NULL,
    name        VARCHAR(255) NOT NULL,
    filename    VARCHAR(255) NOT NULL,
    size        BIGINT       NOT NULL DEFAULT 0,
    uploaded_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_couple FOREIGN KEY (couple_id) REFERENCES couple_profiles(id) ON DELETE CASCADE
);
