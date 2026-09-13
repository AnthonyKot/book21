package lab;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
record Invoice(@Id String id, String tenantId, String customer, long amountCents) {}

@Table
record ExportJob(@Id Long id, String requester, String invoiceId, String status, String content) {
    ExportJob withResult(String status, String content) {
        return new ExportJob(id, requester, invoiceId, status, content);
    }
}
