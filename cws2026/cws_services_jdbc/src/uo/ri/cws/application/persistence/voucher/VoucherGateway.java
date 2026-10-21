package uo.ri.cws.application.persistence.voucher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface VoucherGateway extends Gateway<VoucherGateway.VoucherRecord> {

    Optional<VoucherRecord> findByCode(String code) throws PersistenceException;

    List<VoucherRecord> findByClientNif(String nif) throws PersistenceException;

    void removeForClient(String clientId) throws PersistenceException;

    List<SummaryRecord> findSummary() throws PersistenceException;

    List<EligibleRecord> findEligibleWorkOrders() throws PersistenceException;

    List<EligibleRecord> findEligibleRecommendations() throws PersistenceException;

    List<EligibleRecord> findEligibleInvoices() throws PersistenceException;

    void markWorkOrderUsed(String id) throws PersistenceException;

    void markRecommendationUsed(String id) throws PersistenceException;

    void markInvoiceUsed(String id) throws PersistenceException;

    void addGenerated(String clientId, BigDecimal amount, String description)
            throws PersistenceException;

    record EligibleRecord(String clientId, String itemId) {}

    record SummaryRecord(String nif, String name, String surname, long issuedQty,
            BigDecimal totalAmount, BigDecimal availableAmount,
            BigDecimal consumedAmount) {}
    
    class VoucherRecord extends Gateway.BaseRecord {
        public String clientId;
        public BigDecimal accumulated;
        public String code;
        public String description;
        public BigDecimal available;
    }

}
