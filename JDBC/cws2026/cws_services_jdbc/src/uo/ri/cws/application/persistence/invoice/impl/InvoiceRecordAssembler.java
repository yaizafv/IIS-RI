package uo.ri.cws.application.persistence.invoice.impl;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class InvoiceRecordAssembler extends BaseRecordAssembler<InvoiceRecord> {

    @Override
    protected InvoiceRecord createRecord() {
        return new InvoiceRecord();
    }

    @Override
    public InvoiceRecord toRecord(ResultSet rs) throws SQLException {
        InvoiceRecord record = super.toRecord(rs);
        Date date = rs.getDate("date");
        record.date = date == null ? null : date.toLocalDate();
        record.number = rs.getLong("number");
        record.state = rs.getString("state");
        record.usedForVoucher = rs.getBoolean("usedForVoucher");
        record.subtotal = rs.getBigDecimal("subtotal");
        record.total = rs.getBigDecimal("total");
        record.vatAmount = rs.getBigDecimal("vatAmount");
        record.vatRate = rs.getBigDecimal("vatRate");
        return record;
    }
}
