package com.portalcomunitario.mstenant.comunidad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComunidadRepository extends JpaRepository<Comunidad, UUID> {
    boolean existsBySlug(String slug);
    Optional<Comunidad> findBySlug(String slug);
    List<Comunidad> findAllByOrderByCreatedAtDesc();
}
