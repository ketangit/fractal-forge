package com.fractalforge.puzzle.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

	Optional<CustomerOrder> findByPublicRef(String publicRef);
}
