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
    public void remove(String id) throws PersistenceException {
        execute("TPAYMENTMEANS_DELETE", id);
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
        return findOne(Queries.getSQLSentence("TPAYMENTMEANS_FIND_FULL_BY_ID"), id);
    }

    @Override
    public List<PaymentMeanRecord> findAll() throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_FIND_FULL"));
             ResultSet rs = pst.executeQuery()) {
            return readAll(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<PaymentMeanRecord> findByClientNif(String nif)
            throws PersistenceException {
        return findMany(Queries.getSQLSentence("TPAYMENTMEANS_FIND_FULL_BY_CLIENT_NIF"),
                nif);
    }

    @Override
    public Optional<PaymentMeanRecord> findByVoucherCode(String code)
            throws PersistenceException {
        return findOne(Queries.getSQLSentence("TPAYMENTMEANS_FIND_FULL_BY_VOUCHER_CODE"),
                code);
    }

    @Override
    public Optional<PaymentMeanRecord> findByCardNumber(String number)
            throws PersistenceException {
        return findOne(Queries.getSQLSentence("TPAYMENTMEANS_FIND_FULL_BY_CARD_NUMBER"),
                number);
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

    @Override
    public void removeForClient(String clientId) {
        execute("TPAYMENTMEANS_DELETE_FOR_CLIENT", clientId);
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
