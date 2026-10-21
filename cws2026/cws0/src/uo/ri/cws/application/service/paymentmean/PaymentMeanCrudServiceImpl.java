package uo.ri.cws.application.service.paymentmean;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.util.command.CommandExecutor;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CardDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.CashDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.PaymentMeanDto;
import uo.ri.cws.application.service.paymentmean.PaymentMeanCrudService.VoucherDto;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.exception.BusinessException;
import uo.ri.util.exception.PersistenceException;

public class PaymentMeanCrudServiceImpl implements PaymentMeanCrudService {

    private static final String PAYMENT_MEANS_BY_ID =
            "SELECT pm.id, pm.version, pm.client_id, pm.accumulated, pm.dtype, "
                    + "cc.number AS card_number, cc.validThru AS card_valid_thru, "
                    + "cc.type AS card_type, v.code AS voucher_code, "
                    + "v.description AS voucher_description, v.available AS voucher_available "
                    + "FROM TPaymentMeans pm LEFT JOIN TCreditCards cc ON cc.id = pm.id "
                    + "LEFT JOIN TVouchers v ON v.id = pm.id WHERE pm.id = ?";

    private static final String PAYMENT_MEANS_BY_CLIENT_NIF = PAYMENT_MEANS_BY_ID
            .replace("WHERE pm.id = ?", "JOIN TClients c ON c.id = pm.client_id WHERE c.nif = ?");

    private final CommandExecutor executor = new CommandExecutor();

    @Override
    public CardDto addCard(CardDto card) throws BusinessException {
        ArgumentChecks.isNotNull(card, "La tarjeta no puede ser nula");
        ArgumentChecks.isNotBlank(card.cardNumber, "El número de tarjeta no puede estar vacío");
        ArgumentChecks.isNotNull(card.cardExpiration, "La fecha de caducidad no puede ser nula");
        ArgumentChecks.isNotBlank(card.cardType, "El tipo de tarjeta no puede estar vacío");
        ArgumentChecks.isNotBlank(card.clientId, "El identificador del cliente no puede estar vacío");
        return execute(() -> {
            Connection connection = Jdbc.getCurrentConnection();
            if (!exists(connection, "SELECT id FROM TClients WHERE id = ?", card.clientId)) {
                throw new BusinessException("No existe el cliente");
            }
            if (exists(connection, "SELECT id FROM TCreditCards WHERE number = ?", card.cardNumber)) {
                throw new BusinessException("Ya existe una tarjeta con ese número");
            }
            if (card.cardExpiration.isBefore(YearMonth.from(LocalDate.now()))) {
                throw new BusinessException("La tarjeta está caducada");
            }

            String id = UUID.randomUUID().toString();
            Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TPAYMENTMEANS_ADD"))) {
                bindPaymentMean(pst, id, "CreditCard", now, card.clientId,
                        BigDecimal.ZERO);
                pst.executeUpdate();
            }
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TCREDITCARDS_ADD"))) {
                pst.setString(1, id);
                pst.setString(2, card.cardType);
                pst.setDate(3, Date.valueOf(card.cardExpiration.atEndOfMonth()));
                pst.setString(4, card.cardNumber);
                pst.executeUpdate();
            }
            card.id = id;
            card.version = 1;
            card.accumulatedAmount = BigDecimal.ZERO;
            return card;
        });
    }

    @Override
    public VoucherDto addVoucher(VoucherDto voucher) throws BusinessException {
        ArgumentChecks.isNotNull(voucher, "El vale no puede ser nulo");
        ArgumentChecks.isNotBlank(voucher.code, "El código del vale no puede estar vacío");
        ArgumentChecks.isNotBlank(voucher.description, "La descripción no puede estar vacía");
        ArgumentChecks.isNotBlank(voucher.clientId, "El identificador del cliente no puede estar vacío");
        ArgumentChecks.isNotNull(voucher.availableAmount, "El importe disponible no puede ser nulo");
        ArgumentChecks.isTrue(voucher.availableAmount.signum() >= 0,
                "El importe disponible no puede ser negativo");

        return execute(() -> {
            Connection connection = Jdbc.getCurrentConnection();
            if (!exists(connection, "SELECT id FROM TClients WHERE id = ?", voucher.clientId)) {
                throw new BusinessException("No existe el cliente");
            }
            if (exists(connection, "SELECT id FROM TVouchers WHERE code = ?", voucher.code)) {
                throw new BusinessException("Ya existe un vale con ese código");
            }

            String id = UUID.randomUUID().toString();
            Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
            BigDecimal accumulated = voucher.accumulatedAmount == null
                    ? BigDecimal.ZERO : voucher.accumulatedAmount;
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TPAYMENTMEANS_ADD"))) {
                bindPaymentMean(pst, id, "Voucher", now, voucher.clientId, accumulated);
                pst.executeUpdate();
            }
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TVOUCHERS_ADD"))) {
                pst.setString(1, id);
                pst.setString(2, voucher.description);
                pst.setBigDecimal(3, voucher.availableAmount);
                pst.setString(4, voucher.code);
                pst.executeUpdate();
            }
            voucher.id = id;
            voucher.version = 1;
            voucher.accumulatedAmount = accumulated;
            return voucher;
        });
    }

    @Override
    public void deletePaymentMean(String id) throws BusinessException {
        ArgumentChecks.isNotBlank(id, "El identificador del medio de pago no puede estar vacío");
        execute(() -> {
            Connection connection = Jdbc.getCurrentConnection();
            String dtype;
            try (PreparedStatement pst = connection.prepareStatement(
                    "SELECT dtype FROM TPaymentMeans WHERE id = ?")) {
                pst.setString(1, id);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        throw new BusinessException("No existe el medio de pago");
                    }
                    dtype = rs.getString("dtype");
                }
            }
            if ("Cash".equals(dtype)) {
                throw new BusinessException("No se puede eliminar el medio de pago en efectivo");
            }
            if (exists(connection, "SELECT id FROM TCharges WHERE paymentMean_id = ?", id)) {
                throw new BusinessException("El medio de pago tiene cargos asociados");
            }

            String subtype = "CreditCard".equals(dtype) ? "TCREDITCARDS"
                    : "Voucher".equals(dtype) ? "TVOUCHERS" : null;
            if (subtype != null) {
                try (PreparedStatement pst = connection.prepareStatement(
                        "DELETE FROM " + subtype + " WHERE id = ?")) {
                    pst.setString(1, id);
                    pst.executeUpdate();
                }
            }
            try (PreparedStatement pst = connection.prepareStatement(
                    Queries.getSQLSentence("TPAYMENTMEANS_DELETE"))) {
                pst.setString(1, id);
                pst.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<PaymentMeanDto> findById(String id) throws BusinessException {
        ArgumentChecks.isNotBlank(id, "El identificador del medio de pago no puede estar vacío");
        return execute(() -> {
            try (PreparedStatement pst = Jdbc.getCurrentConnection()
                    .prepareStatement(PAYMENT_MEANS_BY_ID)) {
                pst.setString(1, id);
                try (ResultSet rs = pst.executeQuery()) {
                    return rs.next() ? Optional.of(toDto(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<PaymentMeanDto> findPaymentMeansByClientNif(String nif)
            throws BusinessException {
        ArgumentChecks.isNotBlank(nif, "El NIF del cliente no puede estar vacío");
        return execute(() -> {
            List<PaymentMeanDto> result = new ArrayList<>();
            try (PreparedStatement pst = Jdbc.getCurrentConnection()
                    .prepareStatement(PAYMENT_MEANS_BY_CLIENT_NIF)) {
                pst.setString(1, nif);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        result.add(toDto(rs));
                    }
                }
            }
            return result;
        });
    }

    @Override
    public Optional<VoucherDto> findVoucherByCode(String code) throws BusinessException {
        ArgumentChecks.isNotBlank(code, "El código del vale no puede estar vacío");
        return execute(() -> {
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    PAYMENT_MEANS_BY_ID.replace("WHERE pm.id = ?",
                            "WHERE v.code = ? AND pm.id = v.id"))) {
                pst.setString(1, code);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of((VoucherDto) toDto(rs));
                }
            }
        });
    }

    @Override
    public Optional<CardDto> findCreditCardByNumber(String cardNumber)
            throws BusinessException {
        ArgumentChecks.isNotBlank(cardNumber, "El número de tarjeta no puede estar vacío");
        return execute(() -> {
            try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                    PAYMENT_MEANS_BY_ID.replace("WHERE pm.id = ?",
                            "WHERE cc.number = ? AND pm.id = cc.id"))) {
                pst.setString(1, cardNumber);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of((CardDto) toDto(rs));
                }
            }
        });
    }

    private static PaymentMeanDto toDto(ResultSet rs) throws SQLException {
        String dtype = rs.getString("dtype");
        PaymentMeanDto dto;
        if ("CreditCard".equals(dtype)) {
            CardDto card = new CardDto();
            card.cardNumber = rs.getString("card_number");
            Date expiration = rs.getDate("card_valid_thru");
            card.cardExpiration = expiration == null ? null
                    : YearMonth.from(expiration.toLocalDate());
            card.cardType = rs.getString("card_type");
            dto = card;
        } else if ("Voucher".equals(dtype)) {
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
        dto.accumulatedAmount = rs.getBigDecimal("accumulated");
        return dto;
    }

    private static void bindPaymentMean(PreparedStatement pst, String id, String type,
            Timestamp now, String clientId, BigDecimal accumulated) throws SQLException {
        pst.setString(1, id);
        pst.setString(2, type);
        pst.setTimestamp(3, now);
        pst.setTimestamp(4, now);
        pst.setLong(5, 1);
        pst.setString(6, "ENABLED");
        pst.setBigDecimal(7, accumulated);
        pst.setString(8, clientId);
    }

    private static boolean exists(Connection connection, String sql, String value)
            throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private <T> T execute(SqlCommand<T> command) throws BusinessException {
        return executor.execute(() -> {
            try {
                return command.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @FunctionalInterface
    private interface SqlCommand<T> {
        T execute() throws SQLException, BusinessException;
    }
}
