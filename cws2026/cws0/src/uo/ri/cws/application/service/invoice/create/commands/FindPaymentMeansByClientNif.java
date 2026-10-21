package uo.ri.cws.application.service.invoice.create.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.util.command.Command;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.service.invoice.InvoicingService.CardDto;
import uo.ri.cws.application.service.invoice.InvoicingService.CashDto;
import uo.ri.cws.application.service.invoice.InvoicingService.PaymentMeanDto;
import uo.ri.cws.application.service.invoice.InvoicingService.VoucherDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class FindPaymentMeansByClientNif implements Command<List<PaymentMeanDto>> {

    private final String nif;

    public FindPaymentMeansByClientNif(String nif) {
        validateNif(nif);
        this.nif = nif;
    }

    @Override
    public List<PaymentMeanDto> execute() throws BusinessException {
        List<PaymentMeanDto> result = new ArrayList<>();
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_FIND_BY_CLIENT_NIF"))) {
            pst.setString(1, nif);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.add(toDto(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return result;
    }

    static PaymentMeanDto toDto(ResultSet rs) throws SQLException {
        String type = rs.getString("dtype");
        PaymentMeanDto dto;
        if ("CreditCard".equals(type)) {
            CardDto card = new CardDto();
            card.cardNumber = rs.getString("card_number");
            card.cardType = rs.getString("card_type");
            Date validThru = rs.getDate("valid_thru");
            card.cardExpiration = validThru == null
                    ? null : YearMonth.from(validThru.toLocalDate());
            dto = card;
        } else if ("Voucher".equals(type)) {
            VoucherDto voucher = new VoucherDto();
            voucher.code = rs.getString("voucher_code");
            voucher.description = rs.getString("voucher_description");
            voucher.availableAmount = rs.getBigDecimal("voucher_available");
            dto = voucher;
        } else {
            dto = new CashDto();
        }
        dto.id = rs.getString("id");
        dto.version = rs.getLong("version");
        dto.clientId = rs.getString("client_id");
        dto.accumulated = rs.getBigDecimal("accumulated");
        return dto;
    }

    public static void validateNif(String nif) {
        ArgumentChecks.isNotBlank(nif, "El NIF no puede estar vacío");
        ArgumentChecks.isTrue(nif.matches("[0-9]{8}[A-Z]"),
                "El NIF no tiene un formato válido");
        int number = Integer.parseInt(nif.substring(0, 8));
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        ArgumentChecks.isTrue(nif.charAt(8) == letters.charAt(number % 23),
                "La letra del NIF no es válida");
    }
}
