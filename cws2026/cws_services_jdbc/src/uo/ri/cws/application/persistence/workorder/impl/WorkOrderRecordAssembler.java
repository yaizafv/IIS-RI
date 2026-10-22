package uo.ri.cws.application.persistence.workorder.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;

public class WorkOrderRecordAssembler extends BaseRecordAssembler<WorkOrderRecord> {

    public WorkOrderRecordAssembler() {
    }

    @Override
    protected WorkOrderRecord createRecord() {
        return new WorkOrderRecord();
    }

    public WorkOrderRecord resultSetToWorkOrderRecord(ResultSet rs) throws SQLException {
        WorkOrderRecord result = new WorkOrderRecord();

        result.id = rs.getString("id");
        result.version = rs.getLong("version");
        result.vehicleId = rs.getString("vehicle_id");
        result.description = rs.getString("description");
        Timestamp date = rs.getTimestamp("date");
        result.date = date == null ? null : date.toLocalDateTime();
        result.amount = rs.getBigDecimal("total_amount");
        result.state = rs.getString("state");
        result.mechanicId = rs.getString("mechanic_id");
        result.invoiceId = rs.getString("invoice_id");

        return result;
    }

    public Optional<WorkOrderRecord> toWorkOrderRecord(ResultSet m) throws SQLException {
        return m.next() ? Optional.of(resultSetToWorkOrderRecord(m)) : Optional.ofNullable(null);
    }

    public List<WorkOrderRecord> toWorkOrderRecordList(ResultSet rs) throws SQLException {
        List<WorkOrderRecord> res = new ArrayList<>();
        while (rs.next()) {
            res.add(resultSetToWorkOrderRecord(rs));
        }
        return res;
    }

}
