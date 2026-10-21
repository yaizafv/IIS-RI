package uo.ri.cws.application.persistence.paymentmean.impl;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CashRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.VoucherRecord;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.util.exception.PersistenceException;

public class PaymentMeanGatewayImpl implements PaymentMeanGateway {

    private static final String FULL_SELECT =
            "SELECT pm.id, pm.version, pm.createdAt, pm.updatedAt, pm.entityState, "
                    + "pm.client_id, pm.accumulated, pm.dtype, cc.number AS card_number, "
                    + "cc.validThru AS card_valid_thru, cc.type AS card_type, "
                    + "v.code AS voucher_code, v.description AS voucher_description, "
                    + "v.available AS voucher_available "
                    + "FROM TPaymentMeans pm LEFT JOIN TCreditCards cc ON cc.id = pm.id "
                    + "LEFT JOIN TVouchers v ON v.id = pm.id ";

    @Override
    public void add(PaymentMeanRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_ADD"))) {
            bindCommon(pst, record);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void addCard(CardRecord record) throws PersistenceException {
        add(record);
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TCREDITCARDS_ADD"))) {
            pst.setString(1, record.id);
            pst.setString(2, record.type);
            pst.setDate(3, Date.valueOf(record.validThru.atEndOfMonth()));
            pst.setString(4, record.number);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void addVoucher(VoucherRecord record) throws PersistenceException {
        add(record);
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_ADD"))) {
            pst.setString(1, record.id);
            pst.setString(2, record.description);
            pst.setBigDecimal(3, record.available);
            pst.setString(4, record.code);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        execute("TPAYMENTMEANS_DELETE", id);
    }

    @Override
    public void removeSubtype(PaymentMeanRecord record) throws PersistenceException {
        if ("CreditCard".equals(record.dtype)) {
            execute("TCREDITCARDS_DELETE", record.id);
        } else if ("Voucher".equals(record.dtype)) {
            execute("TVOUCHERS_DELETE", record.id);
        }
    }

    @Override
    public void update(PaymentMeanRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_UPDATE"))) {
            pst.setBigDecimal(1, record.accumulated);
            pst.setTimestamp(2, Timestamp.valueOf(record.updatedAt));
            pst.setString(3, record.id);
            pst.setLong(4, record.version);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(new SQLException("Payment mean was not updated"));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<PaymentMeanRecord> findById(String id) throws PersistenceException {
        return findOne(FULL_SELECT + "WHERE pm.id = ?", id);
    }

    @Override
    public List<PaymentMeanRecord> findAll() throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                FULL_SELECT);
             ResultSet rs = pst.executeQuery()) {
            return readAll(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<PaymentMeanRecord> findByClientNif(String nif)
            throws PersistenceException {
        return findMany(FULL_SELECT
                + "JOIN TClients c ON c.id = pm.client_id WHERE c.nif = ?", nif);
    }

    @Override
    public Optional<PaymentMeanRecord> findByVoucherCode(String code)
            throws PersistenceException {
        return findOne(FULL_SELECT
                + "WHERE v.code = ? AND pm.id = v.id", code);
    }

    @Override
    public Optional<PaymentMeanRecord> findByCardNumber(String number)
            throws PersistenceException {
        return findOne(FULL_SELECT
                + "WHERE cc.number = ? AND pm.id = cc.id", number);
    }

    @Override
    public boolean existsClient(String clientId) throws PersistenceException {
        return exists("TCLIENTS_EXISTS_BY_ID", clientId);
    }

    @Override
    public boolean existsCardNumber(String number) throws PersistenceException {
        return exists("TCREDITCARDS_EXISTS_BY_NUMBER", number);
    }

    @Override
    public boolean existsVoucherCode(String code) throws PersistenceException {
        return exists("TVOUCHERS_EXISTS_BY_CODE", code);
    }

    @Override
    public boolean hasCharges(String id) throws PersistenceException {
        return exists("TCHARGES_EXISTS_BY_PAYMENT_MEAN", id);
    }

    @Override
    public void addAccumulated(String id, java.math.BigDecimal amount)
            throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_ADD_ACCUMULATED"))) {
            pst.setBigDecimal(1, amount);
            pst.setString(2, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void decreaseVoucherAvailable(String id, java.math.BigDecimal amount)
            throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_DECREASE_AVAILABLE"))) {
            pst.setBigDecimal(1, amount);
            pst.setString(2, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private Optional<PaymentMeanRecord> findOne(String sql, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(sql)) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? Optional.of(toRecord(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private List<PaymentMeanRecord> findMany(String sql, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(sql)) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return readAll(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static List<PaymentMeanRecord> readAll(ResultSet rs) throws SQLException {
        List<PaymentMeanRecord> result = new ArrayList<>();
        while (rs.next()) {
            result.add(toRecord(rs));
        }
        return result;
    }

    private static PaymentMeanRecord toRecord(ResultSet rs) throws SQLException {
        String type = rs.getString("dtype");
        PaymentMeanRecord record;
        if ("CreditCard".equals(type)) {
            CardRecord card = new CardRecord();
            card.number = rs.getString("card_number");
            card.type = rs.getString("card_type");
            Date validThru = rs.getDate("card_valid_thru");
            card.validThru = validThru == null ? null
                    : YearMonth.from(validThru.toLocalDate());
            record = card;
        } else if ("Voucher".equals(type)) {
            VoucherRecord voucher = new VoucherRecord();
            voucher.code = rs.getString("voucher_code");
            voucher.description = rs.getString("voucher_description");
            voucher.available = rs.getBigDecimal("voucher_available");
            record = voucher;
        } else {
            record = new CashRecord();
        }
        record.id = rs.getString("id");
        record.version = rs.getLong("version");
        record.createdAt = rs.getTimestamp("createdAt").toLocalDateTime();
        record.updatedAt = rs.getTimestamp("updatedAt").toLocalDateTime();
        record.entityState = rs.getString("entityState");
        record.clientId = rs.getString("client_id");
        record.accumulated = rs.getBigDecimal("accumulated");
        record.dtype = type;
        return record;
    }

    private static void bindCommon(PreparedStatement pst, PaymentMeanRecord record)
            throws SQLException {
        pst.setString(1, record.id);
        pst.setString(2, record.dtype);
        pst.setTimestamp(3, Timestamp.valueOf(record.createdAt));
        pst.setTimestamp(4, Timestamp.valueOf(record.updatedAt));
        pst.setLong(5, record.version);
        pst.setString(6, record.entityState);
        pst.setBigDecimal(7, record.accumulated);
        pst.setString(8, record.clientId);
    }

    private static boolean exists(String key, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static void execute(String key, String id) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence(key))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
