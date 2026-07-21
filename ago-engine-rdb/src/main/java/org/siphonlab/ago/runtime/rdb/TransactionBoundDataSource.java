package org.siphonlab.ago.runtime.rdb;

import org.apache.commons.dbcp2.DelegatingConnection;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

// once get connection, begin transaction, and close won't end close it, to close the connection, invoke datasource.close
// for adapter usage
public class TransactionBoundDataSource implements DataSource{

    private final DataSource dataSource;
    private final boolean autoStartTransaction;

    private TransactionBoundDelegatingConnection connection;

    public TransactionBoundDataSource(DataSource dataSource, boolean autoStartTransaction){
        this.dataSource = dataSource;
        this.autoStartTransaction = autoStartTransaction;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if(connection != null) return connection;
        return connection = new TransactionBoundDelegatingConnection(dataSource.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if(connection != null) return connection;
        connection = new TransactionBoundDelegatingConnection(dataSource.getConnection(username, password));
        if(autoStartTransaction) connection.setAutoCommit(false);
        return connection;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    public void close() throws SQLException {
        if(connection != null && !connection.isClosed()){
            try {
                connection.commit();
            } finally {
                if(!(this.dataSource instanceof TransactionBoundDataSource)) {      // the connection is still using by inner TransactionBoundDataSource
                    connection.inner.close();
                }
            }
        }
    }

    public void commit() throws SQLException {
        if(connection != null){
            connection.commit();
        }
    }

    public void rollback() throws SQLException {
        if(connection != null){
            connection.rollback();
        }
    }

    private static class TransactionBoundDelegatingConnection extends DelegatingConnection<Connection> {
        private final Connection inner;

        public TransactionBoundDelegatingConnection(Connection inner) throws SQLException {
            super(inner);
            this.inner = inner;
        }

        public Connection getInner() {
            return inner;
        }

        @Override
        public void close() throws SQLException {
            // do nothing
        }
    }
}
