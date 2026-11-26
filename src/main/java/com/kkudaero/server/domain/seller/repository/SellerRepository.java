package com.kkudaero.server.domain.seller.repository;

import java.util.Optional;

import com.kkudaero.server.domain.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    boolean existsByEmail(String email);

    Optional<Seller> findByEmail(String email);
}
