package com.graduationproject.backend.repository;

import com.graduationproject.backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Invoice findByOrderOrderId(long orderId);
}
