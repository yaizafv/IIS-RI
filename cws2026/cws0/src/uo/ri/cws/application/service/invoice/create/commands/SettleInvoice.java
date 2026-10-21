package uo.ri.cws.application.service.invoice.create.commands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import uo.ri.conf.Factories;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class SettleInvoice implements Command<Void> {

    private final InvoiceGateway invoiceGateway = Factories.persistence.forInvoice();
    private final String invoiceId;
    private final Map<String, BigDecimal> charges;

    public SettleInvoice(String invoiceId, Map<String, BigDecimal> charges) {
        ArgumentChecks.isNotBlank(invoiceId, "El identificador de factura no puede estar vacío");
        ArgumentChecks.isNotNull(charges, "Los cargos no pueden ser nulos");
        ArgumentChecks.isTrue(!charges.isEmpty(), "Debe indicarse al menos un cargo");
        for (Map.Entry<String, BigDecimal> charge : charges.entrySet()) {
            ArgumentChecks.isNotBlank(charge.getKey(),
                    "El identificador del medio de pago no puede estar vacío");
            ArgumentChecks.isNotNull(charge.getValue(), "El importe del cargo no puede ser nulo");
            ArgumentChecks.isTrue(charge.getValue().signum() >= 0,
                    "El importe del cargo no puede ser negativo");
        }
        this.invoiceId = invoiceId;
        this.charges = new LinkedHashMap<>();
        charges.forEach((id, amount) ->
                this.charges.put(id, amount.setScale(2, RoundingMode.HALF_EVEN)));
    }

    @Override
    public Void execute() throws BusinessException {
        InvoiceRecord invoice = invoiceGateway.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("No existe la factura"));
        if ("PAID".equals(invoice.state)) {
            throw new BusinessException("La factura ya está pagada");
        }

        BigDecimal total = charges.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(invoice.total.setScale(2, RoundingMode.HALF_EVEN)) != 0) {
            throw new BusinessException("El importe de los cargos no coincide con la factura");
        }

        try {
            for (Map.Entry<String, BigDecimal> charge : charges.entrySet()) {
                settlePaymentMean(charge.getKey(), charge.getValue());
            }
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    Queries.getSQLSentence("TCHARGES_ADD"))) {
                Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
                for (Map.Entry<String, BigDecimal> charge : charges.entrySet()) {
                    pst.setString(1, UUID.randomUUID().toString());
                    pst.setTimestamp(2, now);
                    pst.setString(3, "ENABLED");
                    pst.setTimestamp(4, now);
                    pst.setLong(5, 1);
                    pst.setBigDecimal(6, charge.getValue());
                    pst.setString(7, invoiceId);
                    pst.setString(8, charge.getKey());
                    pst.addBatch();
                }
                pst.executeBatch();
            }
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    Queries.getSQLSentence("TINVOICES_SET_PAID"))) {
                pst.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
                pst.setString(2, invoiceId);
                if (pst.executeUpdate() != 1) {
                    throw new BusinessException("No se pudo actualizar el estado de la factura");
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return null;
    }

    private void settlePaymentMean(String id, BigDecimal amount)
            throws SQLException, BusinessException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_FIND_BY_ID"))) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException("No existe el medio de pago");
                }
                String type = rs.getString("dtype");
                if ("CreditCard".equals(type)) {
                    checkCardValid(id);
                } else if ("Voucher".equals(type)) {
                    checkVoucherBalance(id, amount);
                    debitVoucher(id, amount);
                }
            }
        }
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_ADD_ACCUMULATED"))) {
            pst.setBigDecimal(1, amount);
            pst.setString(2, id);
            pst.executeUpdate();
        }
    }

    private void checkCardValid(String id) throws SQLException, BusinessException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TCREDITCARDS_FIND_BY_ID"))) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException("No existe la tarjeta");
                }
                Date validThru = rs.getDate("validThru");
                if (validThru != null
                        && YearMonth.from(validThru.toLocalDate())
                                .isBefore(YearMonth.from(LocalDate.now()))) {
                    throw new BusinessException("La tarjeta está caducada");
                }
            }
        }
    }

    private void checkVoucherBalance(String id, BigDecimal amount)
            throws SQLException, BusinessException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_FIND_BY_ID"))) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException("No existe el vale");
                }
                if (rs.getBigDecimal("available").compareTo(amount) < 0) {
                    throw new BusinessException("El vale no tiene saldo suficiente");
                }
            }
        }
    }

    private void debitVoucher(String id, BigDecimal amount) throws SQLException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_DECREASE_AVAILABLE"))) {
            pst.setBigDecimal(1, amount);
            pst.setString(2, id);
            pst.executeUpdate();
        }
    }
}
