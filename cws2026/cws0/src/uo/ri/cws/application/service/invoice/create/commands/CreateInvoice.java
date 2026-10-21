package uo.ri.cws.application.service.invoice.create.commands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.service.invoice.InvoicingService.InvoiceDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class CreateInvoice implements Command<InvoiceDto> {

    private final List<String> workOrderIds;

    public CreateInvoice(List<String> workOrderIds) {
        ArgumentChecks.isNotNull(workOrderIds, "La lista de órdenes de trabajo no puede ser nula");
        ArgumentChecks.isTrue(!workOrderIds.isEmpty(), "La lista de órdenes de trabajo no puede estar vacía");
        for (String workOrderId : workOrderIds) {
            ArgumentChecks.isNotBlank(workOrderId, "El identificador de la orden no puede estar vacío");
        }
        this.workOrderIds = List.copyOf(workOrderIds);
    }

    @Override
    public InvoiceDto execute() throws BusinessException {
        InvoiceDto dto = new InvoiceDto();
        dto.id = UUID.randomUUID().toString();
        dto.version = 1;
        dto.date = LocalDate.now();
        dto.state = "ISSUED";
        dto.vatPercentage = new BigDecimal("21");

        BigDecimal subtotal = BigDecimal.ZERO;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Connection connection = Jdbc.getCurrentConnection();

        try {
            for (String workOrderId : workOrderIds) {
                try (PreparedStatement pst = connection.prepareStatement(
                        Queries.getSQLSentence("TWORKORDERS_FIND_FOR_INVOICE"))) {
                    pst.setString(1, workOrderId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            throw new BusinessException("La orden de trabajo no existe: " + workOrderId);
                        }

                        String state = rs.getString("state");
                        if (!"APPROVED".equals(state)) {
                            throw new BusinessException(
                                    "La orden de trabajo no se puede facturar en estado: " + state);
                        }

                        BigDecimal amount = rs.getBigDecimal("total_amount");
                        if (amount != null) {
                            subtotal = subtotal.add(amount);
                        }
                    }
                }
            }

            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TINVOICES_FIND_LAST_NUMBER"));
                 ResultSet rs = pst.executeQuery()) {
                long lastNumber = 0;
                if (rs.next()) {
                    lastNumber = rs.getLong(1);
                    if (rs.wasNull()) {
                        lastNumber = 0;
                    }
                }
                dto.number = lastNumber + 1;
            }

            dto.subtotal = subtotal.setScale(2, RoundingMode.HALF_EVEN);
            dto.vatAmount = dto.subtotal.multiply(dto.vatPercentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
            dto.total = dto.subtotal.add(dto.vatAmount).setScale(2, RoundingMode.HALF_UP);

            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TINVOICES_INSERT_FOR_INVOICE"))) {
                pst.setString(1, dto.id);
                pst.setLong(2, dto.version);
                pst.setLong(3, dto.number);
                pst.setDate(4, java.sql.Date.valueOf(dto.date));
                pst.setString(5, dto.state);
                pst.setBoolean(6, false);
                pst.setBigDecimal(7, dto.subtotal);
                pst.setBigDecimal(8, dto.total);
                pst.setBigDecimal(9, dto.vatAmount);
                pst.setBigDecimal(10, dto.vatPercentage);
                pst.setTimestamp(11, now);
                pst.setTimestamp(12, now);
                pst.setString(13, "ENABLED");
                pst.executeUpdate();
            }

            for (String workOrderId : workOrderIds) {
                try (PreparedStatement pst = connection.prepareStatement(
                        Queries.getSQLSentence("TWORKORDERS_UPDATE_FOR_INVOICE"))) {
                    pst.setString(1, dto.id);
                    pst.setTimestamp(2, now);
                    pst.setString(3, workOrderId);
                    pst.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return dto;
    }
}
