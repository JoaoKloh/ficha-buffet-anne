package com.jbkloh.ficha_buffet.repositories;

import com.jbkloh.ficha_buffet.model.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {
}
