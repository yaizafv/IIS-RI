package uo.ri.cws.application.persistence.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import uo.ri.cws.application.persistence.Gateway;
import uo.ri.util.exception.PersistenceException;

public interface InvoiceGateway extends Gateway<InvoiceGateway.InvoiceRecord> {

    Optional<InvoiceRecord> findByNumber(Long number) throws PersistenceException;

    Optional<Long> findLastNumber() throws PersistenceException;

    void setPaid(String id) throws PersistenceException;

    public class InvoiceRecord extends Gateway.BaseRecord {
        public LocalDate date;
        public long number;
        public String state;
        public BigDecimal subtotal;
        public BigDecimal total;
        public BigDecimal vatAmount;
        public BigDecimal vatRate;
    }
}
