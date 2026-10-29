package uo.ri.cws.application.persistence.workorder.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderRecord;

public class WorkOrderRecordAssembler extends BaseRecordAssembler<WorkOrderRecord> {

    @Override
    protected WorkOrderRecord createRecord() {
        return new WorkOrderRecord();
    }

    @Override
    public WorkOrderRecord toRecord(ResultSet rs) throws SQLException {
        WorkOrderRecord result = super.toRecord(rs);
        result.vehicleId = rs.getString("vehicle_id");
        result.description = rs.getString("description");
        Timestamp date = rs.getTimestamp("date");
        result.date = date == null ? null : date.toLocalDateTime();
        result.amount = rs.getBigDecimal("total_amount");
        result.state = rs.getString("state");
        result.usedForVoucher = rs.getBoolean("usedForVoucher");
        result.mechanicId = rs.getString("mechanic_id");
        result.invoiceId = rs.getString("invoice_id");
        return result;
    }

}
