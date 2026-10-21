package uo.ri.cws.application.persistence.paymentmean;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface PaymentMeanGateway extends Gateway<PaymentMeanGateway.PaymentMeanRecord> {

    Optional<PaymentMeanRecord> findById(String id) throws PersistenceException;

    List<PaymentMeanRecord> findByClientNif(String nif) throws PersistenceException;

    Optional<PaymentMeanRecord> findByVoucherCode(String code) throws PersistenceException;

    Optional<PaymentMeanRecord> findByCardNumber(String number) throws PersistenceException;

    boolean existsClient(String clientId) throws PersistenceException;

    boolean existsCardNumber(String number) throws PersistenceException;

    boolean existsVoucherCode(String code) throws PersistenceException;

    boolean hasCharges(String id) throws PersistenceException;

    void addAccumulated(String id, BigDecimal amount) throws PersistenceException;

    void decreaseVoucherAvailable(String id, BigDecimal amount)
            throws PersistenceException;

    void removeForClient(String clientId) throws PersistenceException;

    class PaymentMeanRecord extends Gateway.BaseRecord {
        public String clientId;
        public BigDecimal accumulated;
        public String dtype;
    }

    class CashRecord extends PaymentMeanRecord {}

    class CardRecord extends PaymentMeanRecord {
        public String number;
        public YearMonth validThru;
        public String type;
    }

    class VoucherRecord extends PaymentMeanRecord {
        public String code;
        public String description;
        public BigDecimal available;
    }
}
