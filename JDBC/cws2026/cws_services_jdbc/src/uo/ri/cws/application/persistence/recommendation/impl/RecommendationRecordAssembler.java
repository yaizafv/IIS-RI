package uo.ri.cws.application.persistence.recommendation.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.cws.application.persistence.recommendation.RecommendationGateway.RecommendationRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class RecommendationRecordAssembler extends BaseRecordAssembler<RecommendationRecord> {

    @Override
    protected RecommendationRecord createRecord() {
        return new RecommendationRecord();
    }

    @Override
    public RecommendationRecord toRecord(ResultSet rs) throws SQLException {
        RecommendationRecord record = super.toRecord(rs);
        record.recommenderId = rs.getString("recommender_id");
        record.recommendedId = rs.getString("recommended_id");
        record.usedForVoucher = rs.getBoolean("usedForVoucher");
        return record;
    }
}
