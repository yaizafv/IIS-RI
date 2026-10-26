package uo.ri.cws.application.persistence.paymentmean.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.util.exception.PersistenceException;

public class PaymentMeanGatewayImpl implements PaymentMeanGateway {

    private final PaymentMeanRecordAssembler assembler = new PaymentMeanRecordAssembler();

    @Override
    public void add(PaymentMeanRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TPAYMENTMEANS_ADD"))) {
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
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TPAYMENTMEANS_UPDATE"))) {
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
        try (
            PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence("TPAYMENTMEANS_FIND_FULL"));
            ResultSet rs = pst.executeQuery()) {
            return readAll(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<PaymentMeanRecord> findByClientId(String clientId) throws PersistenceException {
        return findMany(Queries.getSQLSentence("TPAYMENTMEANS_FIND_FULL_BY_CLIENT_ID"), clientId);
    }

    @Override
    public boolean existsClient(String clientId) throws PersistenceException {
        return exists("TCLIENTS_EXISTS_BY_ID", clientId);
    }

    @Override
    public boolean hasCharges(String id) throws PersistenceException {
        return exists("TCHARGES_EXISTS_BY_PAYMENT_MEAN", id);
    }

    @Override
    public void addAccumulated(String id, BigDecimal amount) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TPAYMENTMEANS_ADD_ACCUMULATED"))) {
            pst.setBigDecimal(1, amount);
            pst.setString(2, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void removeForClient(String clientId) throws PersistenceException {
        execute("TPAYMENTMEANS_DELETE_FOR_CLIENT", clientId);
    }

    private Optional<PaymentMeanRecord> findOne(String sql, String value) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(sql)) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                PaymentMeanRecord record = assembler.toRecord(rs);
                return record == null ? Optional.empty() : Optional.of(record);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private List<PaymentMeanRecord> findMany(String sql, String value) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(sql)) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return readAll(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private List<PaymentMeanRecord> readAll(ResultSet rs) throws SQLException {
        return assembler.toRecordList(rs);
    }

    private static void bindCommon(PreparedStatement pst, PaymentMeanRecord record) throws SQLException {
        pst.setString(1, record.id);
        pst.setString(2, record.dtype);
        pst.setTimestamp(3, Timestamp.valueOf(record.createdAt));
        pst.setTimestamp(4, Timestamp.valueOf(record.updatedAt));
        pst.setLong(5, record.version);
        pst.setString(6, record.entityState);
        pst.setBigDecimal(7, record.accumulated);
        pst.setString(8, record.clientId);
    }

    private static boolean exists(String key, String value) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static void execute(String key, String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
