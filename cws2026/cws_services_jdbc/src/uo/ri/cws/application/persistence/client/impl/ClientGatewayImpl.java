package uo.ri.cws.application.persistence.client.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.client.ClientGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.util.exception.PersistenceException;

public class ClientGatewayImpl implements ClientGateway {

    private final ClientRecordAssembler assembler = new ClientRecordAssembler();

    @Override
    public void add(ClientRecord record) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCLIENTS_ADD"))) {
            pst.setString(1, record.id);
            pst.setLong(2, record.version);
            pst.setTimestamp(3, Timestamp.valueOf(record.createdAt));
            pst.setTimestamp(4, Timestamp.valueOf(record.updatedAt));
            pst.setString(5, record.entityState);
            pst.setString(6, record.name);
            pst.setString(7, record.surname);
            pst.setString(8, record.nif);
            pst.setString(9, record.phone);
            pst.setString(10, record.email);
            pst.setString(11, record.street);
            pst.setString(12, record.city);
            pst.setString(13, record.country);
            pst.setString(14, record.postalCode);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) {
        execute("TCLIENTS_DELETE_BY_ID", id);
    }

    @Override
    public void update(ClientRecord record) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCLIENTS_UPDATE"))) {
            pst.setString(1, record.name);
            pst.setString(2, record.surname);
            pst.setString(3, record.nif);
            pst.setString(4, record.phone);
            pst.setString(5, record.email);
            pst.setString(6, record.street);
            pst.setString(7, record.city);
            pst.setString(8, record.country);
            pst.setString(9, record.postalCode);
            pst.setTimestamp(10, Timestamp.valueOf(record.updatedAt));
            pst.setString(11, record.id);
            pst.setLong(12, record.version);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(
                    new SQLException("Client was not updated because its version has changed"));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<ClientRecord> findById(String id) {
        return findOne("TCLIENTS_FIND_BY_ID", id);
    }

    @Override
    public List<ClientRecord> findAll() {
        try (
            PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_ALL"));
            ResultSet rs = pst.executeQuery()) {
            return assembler.toRecordList(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<ClientRecord> findByNif(String nif) {
        return findOne("TCLIENTS_FIND_BY_NIF", nif);
    }

    @Override
    public Optional<ClientRecord> findOtherByNif(String nif, String id) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_OTHER_BY_NIF"))) {
            pst.setString(1, nif);
            pst.setString(2, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                ClientRecord record = new ClientRecord();
                record.id = rs.getString("id");
                return Optional.of(record);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<ClientRecord> findRecommendedBy(String recommenderNif) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCLIENTS_FIND_RECOMMENDED_BY_NIF"))) {
            pst.setString(1, recommenderNif);
            try (ResultSet rs = pst.executeQuery()) {
                return assembler.toRecordList(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public boolean hasVehicles(String clientId) {
        return exists("TVEHICLES_EXISTS_BY_CLIENT", clientId);
    }

    @Override
    public boolean hasPaidInvoices(String clientId) {
        return exists("TCLIENTS_HAS_PAID_INVOICES", clientId);
    }

    private Optional<ClientRecord> findOne(String key, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? Optional.of(assembler.toCurrentRecord(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static boolean exists(String key, String value) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private static void execute(String key, String id) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(Queries.getSQLSentence(key))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
