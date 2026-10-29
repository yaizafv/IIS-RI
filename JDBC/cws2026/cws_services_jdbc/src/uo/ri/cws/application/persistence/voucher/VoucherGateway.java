package uo.ri.cws.application.persistence.voucher;

import java.math.BigDecimal;
import java.util.List;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.util.exception.PersistenceException;

public interface VoucherGateway extends Gateway<VoucherGateway.VoucherRecord> {

    List<VoucherRecord> findByClientId(String clientId) throws PersistenceException;

    class VoucherRecord extends PaymentMeanRecord {
        public String code;
        public String description;
        public BigDecimal available;
    }
}
