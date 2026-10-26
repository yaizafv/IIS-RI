package uo.ri.cws.application.persistence.workorder.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class WorkOrderGatewayImpl implements WorkOrderGateway {

    private final WorkOrderRecordAssembler assembler = new WorkOrderRecordAssembler();

    @Override
    public void add(WorkOrderRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void remove(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void update(WorkOrderRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TWORKORDERS_UPDATE"))) {
            pst.setString(1, record.description);
            pst.setBigDecimal(2, record.amount);
            pst.setString(3, record.state);
            pst.setBoolean(4, record.usedForVoucher);
            pst.setString(5, record.mechanicId);
            pst.setString(6, record.invoiceId);
            pst.setTimestamp(7, Timestamp.valueOf(record.updatedAt));
            pst.setString(8, record.id);
            pst.setLong(9, record.version);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(
                    new SQLException("Work order was not updated because its version has changed"));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<WorkOrderRecord> findById(String id) throws PersistenceException {
        List<WorkOrderRecord> found = findMany("TWORKORDERS_FIND_BY_ID", id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<WorkOrderRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<WorkOrderRecord> findNotUsedForVoucherByInvoiceState(String invoiceState)
        throws PersistenceException {
        return findMany("TWORKORDERS_FIND_NOT_USED_FOR_VOUCHER_BY_INVOICE_STATE", invoiceState);
    }

    @Override
    public List<WorkOrderRecord> findByMechanicId(String mechanicId) throws PersistenceException {
        return findMany("TWORKORDERS_FIND_BY_MECHANIC_ID", mechanicId);
    }

    @Override
    public List<WorkOrderRecord> findByClientNif(String nif) throws PersistenceException {
        return findMany("TWORKORDERS_FIND_BY_CLIENT_NIF", nif);
    }

    @Override
    public List<WorkOrderRecord> findNotInvoicedWorkOrdersByClientNif(String nif) throws PersistenceException {
        return findMany("TWORKORDERS_FIND_NOT_INVOICED_WORK_ORDERS_BY_CLIENT_NIF", nif);
    }

    private List<WorkOrderRecord> findMany(String queryKey, String parameter) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence(queryKey))) {
            pst.setString(1, parameter);
            try (ResultSet rs = pst.executeQuery()) {
                return assembler.toRecordList(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
