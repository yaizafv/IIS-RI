package uo.ri.cws.application.persistence.voucher;

import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.util.exception.PersistenceException;

public interface VoucherGateway extends Gateway<VoucherGateway.VoucherRecord> {

    Optional<VoucherRecord> findByCode(String code) throws PersistenceException;

    List<VoucherRecord> findByClientId(String clientId) throws PersistenceException;

    void removeForClient(String clientId) throws PersistenceException;

    class VoucherRecord extends PaymentMeanRecord {
        public String code;
        public String description;
        public java.math.BigDecimal available;
    }

}
