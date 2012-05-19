package com.alibaba.druid.hbase.exec;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.alibaba.druid.hbase.HBaseConnection;
import com.alibaba.druid.hbase.HBasePreparedStatement;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.visitor.SQLEvalVisitorUtils;

public class InsertExecutePlan extends SingleTableExecutePlan {

    private Map<String, SQLExpr> columns = new LinkedHashMap<String, SQLExpr>();
    private byte[]               family  = Bytes.toBytes("d");

    @Override
    public boolean execute(HBasePreparedStatement statement) throws SQLException {
        try {
            HBaseConnection connection = statement.getConnection();
            HTableInterface htable = connection.getHTable(getTableName());
            String dbType = connection.getConnectProperties().getProperty("dbType");

            Put put = null;
            for (Map.Entry<String, SQLExpr> entry : columns.entrySet()) {
                String column = entry.getKey();
                SQLExpr valueExpr = entry.getValue();

                Object value = SQLEvalVisitorUtils.eval(dbType, valueExpr, statement.getParameters());

                if (value == null) {
                    continue;
                }

                byte[] bytes;

                if (value instanceof String) {
                    String strValue = (String) value;
                    bytes = Bytes.toBytes(strValue);
                } else if (value instanceof Integer) {
                    int intValue = ((Integer) value).intValue();
                    bytes = Bytes.toBytes(intValue);
                } else {
                    throw new SQLException("TODO"); // TODO
                }

                if (put == null) { // first value is key, TODO
                    put = new Put(bytes);
                } else {
                    byte[] qualifier = Bytes.toBytes(column);
                    put.add(family, qualifier, bytes);
                }
            }

            htable.put(put);

            return false;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("executeQuery error", e);
        }
    }

    public Map<String, SQLExpr> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, SQLExpr> columns) {
        this.columns = columns;
    }

}