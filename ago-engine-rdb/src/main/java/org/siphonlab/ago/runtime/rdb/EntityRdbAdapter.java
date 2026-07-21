package org.siphonlab.ago.runtime.rdb;

import groovy.lang.Closure;
import groovy.lang.Tuple;
import groovy.sql.Sql;
import groovy.sql.SqlWithParams;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.dbcp2.Utils.closeQuietly;

// entity adapter support a simple cache, if enabled,
// and it allows to create a transaction bound adapter
public abstract class EntityRdbAdapter<Id> extends RdbAdapter<Id> implements EntityAdapter<Id> {

    private final static Logger LOGGER = LoggerFactory.getLogger(EntityRdbAdapter.class);
    protected final AgoClass entityClass;

    // cache for this session
    protected Map<ObjectRef<Id>, Instance<?>> cache = new ConcurrentHashMap<>();

    private Map<ObjectRef<Id>, RowState> itemsBuffer = new ConcurrentHashMap<>();

    public EntityRdbAdapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, TypeMapping typeMapping, DataSource dataSource) {
        super(classManager, idType, idGenerator, boxTypes, typeMapping, dataSource);
        this.entityClass = classManager.getClass("lang.Entity");
    }

    public ResultSetToEntityMapper<Id> fetchAll(AgoClass agoClass, RunSpace runSpace) {
        var tableOfClass = getTableOfClass(agoClass);

        StringBuilder sql = composeSelectFrom(tableOfClass);

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(sql.toString());

            PreparedStatement finalPs = ps;
            Connection finalConnection = connection;
            return new ResultSetToEntityMapper<Id>(finalPs.executeQuery(), agoClass, tableOfClass, boxTypes, runSpace, idType) {
                public void close() {
                    super.close();
                    closeQuietly(finalPs);
                    closeQuietly(finalConnection);
                }
            };
        } catch (SQLException e) {
            closeQuietly(ps);
            closeQuietly(connection);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResultSetToQueryResultMapper<Id> executeQuery(String sql, Map<String, Object> arguments, AgoClass entityClass, RunSpace runSpace) {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("EXEC Query: " + sql);
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Mutable<ResultSet> rs = new MutableObject<>();
            var manualCloseSql = new ManualCloseSql(connection);
            manualCloseSql.query(sql, arguments, new Closure(null, null) {
                @Override
                public Object call(Object resultSet) {
                    rs.setValue((ResultSet) resultSet);
                    return null;
                }
            });

            return new ResultSetToQueryResultMapper<Id>(rs.get(), entityClass, this, runSpace, idType) {
                public void close() {
                    super.close();
                    manualCloseSql.close();
                }
            };
        } catch (SQLException e) {
            closeQuietly(connection);
            throw new RuntimeException(e);
        }
    }

    static class ManualCloseSql extends Sql{

        private Connection connection;
        private Statement statement;
        private ResultSet results;

        public ManualCloseSql(DataSource dataSource) {
            super(dataSource);
        }

        public ManualCloseSql(Connection connection) {
            super(connection);
        }

        @Override
        protected void closeResources(Connection connection, Statement statement, ResultSet results) {
            this.connection = connection;
            this.statement = statement;
            this.results = results;
        }

        public void close(){
            super.closeResources(connection, statement, results);
            super.close();
        }

        public SqlWithParams checkForNamedParams(String sql, List<?> params) {
            SqlWithParams preCheck = buildSqlWithIndexedProps(sql);
            if (preCheck == null) {
                return new SqlWithParams(sql, Collections.emptyList());
            }

            List<Tuple> indexPropList = new ArrayList<>();
            for (Object next : preCheck.getParams()) {
                indexPropList.add((Tuple) next);
            }
            return new SqlWithParams(preCheck.getSql(), getUpdatedParams(params, indexPropList));
        }
    }

    @Override
    protected void insert(Instance<?> instance, DbSlots<Id> dbSlots, AgoClass agoClass) {
        if(entityClass != agoClass && entityClass.isThatOrSuperOfThat(agoClass)) {
            itemsBuffer.putIfAbsent(dbSlots.getObjectRef(), RowState.Added);
            cache.put(dbSlots.getObjectRef(), instance);
        }
    }

    @Override
    protected void update(Instance<?> instance, DbSlots<Id> dbSlots, AgoClass agoClass) {
        if(entityClass != agoClass && entityClass.isThatOrSuperOfThat(agoClass)) {
            itemsBuffer.putIfAbsent(dbSlots.getObjectRef(), RowState.Modified);
            cache.put(dbSlots.getObjectRef(), instance);
        }
    }

    @Override
    public void flush(RunSpace runSpace) {
        for (var objectRef : itemsBuffer.keySet()) {
            var instance = this.getById(objectRef, runSpace);
            switch (itemsBuffer.get(objectRef)) {
                case Modified:
                    super.update(instance, (DbSlots<Id>) instance.getSlots(), instance.getAgoClass());
                    break;
                case Added:
                    super.insert(instance, (DbSlots<Id>) instance.getSlots(), instance.getAgoClass());
                    break;
            }
        }
        itemsBuffer.clear();
    }

    @Override
    public Instance<?> getById(ObjectRef<Id> objectRef, RunSpace runSpace) {
        return cache.computeIfAbsent(objectRef, r ->{
            return super.getById(objectRef, runSpace);
        });
    }

    @Override
    public boolean isEntityClass(AgoClass agoClass) {
        return !agoClass.isGenericTemplate()
                && !agoClass.isInGenericTemplate()
                && !(agoClass instanceof AgoInterface)
                && agoClass.getSlotDefs().length > 0
                && entityClass.isThatOrSuperOfThat(agoClass);
    }
}
