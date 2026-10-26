package uo.ri.cws.application.persistence.charge.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.cws.application.persistence.charge.ChargeGateway.ChargeRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class ChargeRecordAssembler extends BaseRecordAssembler<ChargeRecord> {

    @Override
    protected ChargeRecord createRecord() {
        return new ChargeRecord();
    }

    @Override
    public ChargeRecord toRecord(ResultSet rs) throws SQLException {
        ChargeRecord record = super.toRecord(rs);
        record.charge = rs.getBigDecimal("charge");
        record.invoiceId = rs.getString("invoice_id");
        record.paymentMeanId = rs.getString("paymentMean_id");
        return record;
    }
}
