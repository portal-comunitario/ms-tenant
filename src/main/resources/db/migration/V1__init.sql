-- Registro global de comunidades (vive en el schema public; ms-tenant lo administra).
CREATE TABLE IF NOT EXISTS comunidad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(160) NOT NULL,
    comuna VARCHAR(120),
    slug VARCHAR(63) NOT NULL UNIQUE,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    admin_email VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
