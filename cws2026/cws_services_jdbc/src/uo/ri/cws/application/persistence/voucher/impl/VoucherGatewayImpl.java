package uo.ri.cws.application.persistence.voucher.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.util.exception.PersistenceException;

public class VoucherGatewayImpl implements VoucherGateway {

    @Override
    public void add(VoucherRecord record) throws PersistenceException {
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
        execute("TVOUCHERS_DELETE", id);
    }

    @Override
    public void update(VoucherRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_UPDATE"))) {
            pst.setString(1, record.description);
            pst.setBigDecimal(2, record.available);
            pst.setString(3, record.code);
            pst.setTimestamp(4, Timestamp.valueOf(record.updatedAt));
            pst.setString(5, record.id);
            pst.setLong(6, record.version);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(new SQLException("Voucher was not updated"));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<VoucherRecord> findById(String id) throws PersistenceException {
        return findOne("TVOUCHERS_FIND_FULL_BY_ID", id);
    }

    @Override
    public Optional<VoucherRecord> findByCode(String code) throws PersistenceException {
        return findOne("TVOUCHERS_FIND_FULL_BY_CODE", code);
    }

    @Override
    public List<VoucherRecord> findByClientNif(String nif) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_FIND_FULL_BY_CLIENT_NIF"))) {
            pst.setString(1, nif);
            try (ResultSet rs = pst.executeQuery()) {
                List<VoucherRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(toRecord(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void removeForClient(String clientId) {
        execute("TVOUCHERS_DELETE_FOR_CLIENT", clientId);
    }

    @Override
    public List<SummaryRecord> findSummary() throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_SUMMARY"));
             ResultSet rs = pst.executeQuery()) {
            List<SummaryRecord> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new SummaryRecord(rs.getString("nif"), rs.getString("name"),
                        rs.getString("surname"), rs.getLong("issuedQty"),
                        rs.getBigDecimal("totalAmount"), rs.getBigDecimal("availableAmount"),
                        rs.getBigDecimal("consumedAmount")));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<EligibleRecord> findEligibleWorkOrders() throws PersistenceException {
        return findEligible("TVOUCHERS_WORKORDERS_ELIGIBLE");
    }

    @Override
    public List<EligibleRecord> findEligibleRecommendations() throws PersistenceException {
        return findEligible("TVOUCHERS_RECOMMENDATIONS_ELIGIBLE");
    }

    @Override
    public List<EligibleRecord> findEligibleInvoices() throws PersistenceException {
        return findEligible("TVOUCHERS_INVOICES_ELIGIBLE");
    }

    @Override
    public void markWorkOrderUsed(String id) throws PersistenceException {
        execute("TWORKORDERS_MARK_USED_FOR_VOUCHER", id);
    }

    @Override
    public void markRecommendationUsed(String id) throws PersistenceException {
        execute("TRECOMMENDATIONS_MARK_USED_FOR_VOUCHER", id);
    }

    @Override
    public void markInvoiceUsed(String id) throws PersistenceException {
        execute("TINVOICES_MARK_USED_FOR_VOUCHER", id);
    }

    @Override
    public void addGenerated(String clientId, BigDecimal amount, String description)
            throws PersistenceException {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement parent = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TPAYMENTMEANS_ADD_GENERATED"));
             PreparedStatement voucher = Jdbc.getCurrentConnection().prepareStatement(
                     Queries.getSQLSentence("TVOUCHERS_ADD"))) {
            parent.setString(1, id);
            parent.setTimestamp(2, Timestamp.valueOf(now));
            parent.setTimestamp(3, Timestamp.valueOf(now));
            parent.setLong(4, 1);
            parent.setString(5, "ENABLED");
            parent.setBigDecimal(6, BigDecimal.ZERO);
            parent.setString(7, clientId);
            parent.executeUpdate();

            voucher.setString(1, id);
            voucher.setString(2, description);
            voucher.setBigDecimal(3, amount);
            voucher.setString(4, "V-" + UUID.randomUUID());
            voucher.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<VoucherRecord> findAll() throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TVOUCHERS_FIND_FULL"));
             ResultSet rs = pst.executeQuery()) {
            List<VoucherRecord> result = new ArrayList<>();
            while (rs.next()) {
                result.add(toRecord(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private Optional<VoucherRecord> findOne(String key, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? Optional.of(toRecord(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static VoucherRecord toRecord(ResultSet rs) throws SQLException {
        VoucherRecord record = new VoucherRecord();
        record.id = rs.getString("id");
        record.version = rs.getLong("version");
        record.createdAt = rs.getTimestamp("createdAt").toLocalDateTime();
        record.updatedAt = rs.getTimestamp("updatedAt").toLocalDateTime();
        record.entityState = rs.getString("entityState");
        record.clientId = rs.getString("client_id");
        record.accumulated = rs.getBigDecimal("accumulated");
        record.code = rs.getString("code");
        record.description = rs.getString("description");
        record.available = rs.getBigDecimal("available");
        return record;
    }

    private List<EligibleRecord> findEligible(String key) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence(key));
             ResultSet rs = pst.executeQuery()) {
            List<EligibleRecord> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new EligibleRecord(rs.getString(1), rs.getString(2)));
            }
            return result;
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
