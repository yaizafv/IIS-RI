package uo.ri.cws.application.persistence.invoice.impl;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class InvoiceRecordAssembler extends BaseRecordAssembler<InvoiceRecord> {

    public InvoiceRecordAssembler() {
        super(InvoiceRecord::new);
    }

    @Override
    public InvoiceRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    private InvoiceRecord toCurrentRecord(ResultSet rs) throws SQLException {
        var record = super.toRecord(rs);
        
        Date date = rs.getDate("date");
        record.date = date == null ? null : date.toLocalDate();
        record.number = rs.getLong("number");
        record.state = rs.getString("state");
        record.subtotal = rs.getBigDecimal("subtotal");
        record.total = rs.getBigDecimal("total");
        record.vatamount = rs.getBigDecimal("vatamount");
        record.vatrate = rs.getBigDecimal("vatrate");

        return record;
    }

    public List<InvoiceRecord> toInvoiceRecordList(ResultSet rs) throws SQLException {
        List<InvoiceRecord> res = new ArrayList<>();
        while (rs.next()) {
            res.add(toCurrentRecord(rs));
        }
        return res;
    }

}