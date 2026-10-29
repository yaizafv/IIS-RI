package uo.ri.cws.application.persistence.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface InvoiceGateway extends Gateway<InvoiceGateway.InvoiceRecord> {

    Optional<Long> findLastNumber() throws PersistenceException;

    List<InvoiceRecord> findByPayerId(String clientId) throws PersistenceException;

    public class InvoiceRecord extends Gateway.BaseRecord {
        public LocalDate date;
        public long number;
        public String state;
        public boolean usedForVoucher;
        public BigDecimal subtotal;
        public BigDecimal total;
        public BigDecimal vatAmount;
        public BigDecimal vatRate;
    }
}
