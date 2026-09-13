package lab;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

interface InvoiceRepository extends ListCrudRepository<Invoice, String> {
    List<Invoice> findByTenantId(String tenantId);
    Optional<Invoice> findByIdAndTenantId(String id, String tenantId);
}

interface ExportJobRepository extends ListCrudRepository<ExportJob, Long> {
    List<ExportJob> findByStatus(String status);
    Optional<ExportJob> findByIdAndRequester(Long id, String requester);
}
