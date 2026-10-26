package uo.ri.cws.application.persistence.voucher.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.cws.application.persistence.voucher.VoucherGateway;
import uo.ri.util.exception.PersistenceException;

public class VoucherGatewayImpl implements VoucherGateway {

    private final VoucherRecordAssembler assembler = new VoucherRecordAssembler();

    @Override
    public void add(VoucherRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TVOUCHERS_ADD"))) {
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
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TVOUCHERS_UPDATE"))) {
            pst.setString(1, record.description);
            pst.setBigDecimal(2, record.available);
            pst.setString(3, record.code);
            pst.setString(4, record.id);
            pst.executeUpdate();
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
    public List<VoucherRecord> findByClientId(String clientId) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TVOUCHERS_FIND_FULL_BY_CLIENT_ID"))) {
            pst.setString(1, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                List<VoucherRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(assembler.toCurrentRecord(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void removeForClient(String clientId) throws PersistenceException {
        execute("TVOUCHERS_DELETE_FOR_CLIENT", clientId);
    }

    @Override
    public List<VoucherRecord> findAll() throws PersistenceException {
        try (
            PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence("TVOUCHERS_FIND_FULL"));
            ResultSet rs = pst.executeQuery()) {
            List<VoucherRecord> result = new ArrayList<>();
            while (rs.next()) {
                result.add(assembler.toCurrentRecord(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private Optional<VoucherRecord> findOne(String key, String value) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                VoucherRecord record = assembler.toRecord(rs);
                return record == null ? Optional.empty() : Optional.of(record);
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
