/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.persistence.jdbc.internal.db;

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.knowm.yank.Yank;
import org.knowm.yank.exceptions.YankSQLException;
import org.openhab.core.items.Item;
import org.openhab.core.types.State;
import org.openhab.persistence.jdbc.internal.dto.ItemVO;
import org.openhab.persistence.jdbc.internal.dto.ItemsVO;
import org.openhab.persistence.jdbc.internal.exceptions.JdbcSQLException;
import org.openhab.persistence.jdbc.internal.utils.StringUtilsExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended Database Configuration class. Class represents
 * the extended database-specific configuration. Overrides and supplements the
 * default settings from JdbcBaseDAO. Enter only the differences to JdbcBaseDAO here.
 *
 * @author Helmut Lehmeyer - Initial contribution
 */
@NonNullByDefault
public class JdbcSqliteDAO extends JdbcBaseDAO {
    private static final String DRIVER_CLASS_NAME = org.sqlite.JDBC.class.getName();
    @SuppressWarnings("unused")
    private static final String DATA_SOURCE_CLASS_NAME = org.sqlite.SQLiteDataSource.class.getName();

    private final Logger logger = LoggerFactory.getLogger(JdbcSqliteDAO.class);

    private static final String DATETIME_FORMAT = "'%Y-%m-%d %H:%M:%f'";

    /********
     * INIT *
     ********/
    public JdbcSqliteDAO() {
        initSqlQueries();
        initSqlTypes();
        initDbProps();
    }

    private void initSqlQueries() {
        logger.debug("JDBC::initSqlQueries: '{}'", this.getClass().getSimpleName());
        sqlGetDB = "PRAGMA DATABASE_LIST"; // "SELECT SQLITE_VERSION()"; // "PRAGMA DATABASE_LIST"->db Path/Name
                                           // "PRAGMA SCHEMA_VERSION";
        sqlIfTableExists = "SELECT name FROM sqlite_master WHERE type='table' AND name='#searchTable#'";
        sqlCreateItemsTableIfNot = "CREATE TABLE IF NOT EXISTS #itemsManageTable# (ItemId INTEGER PRIMARY KEY AUTOINCREMENT, #colname# #coltype# NOT NULL)";
        sqlGetItemTables = "SELECT name AS table_name FROM sqlite_master WHERE type='table' AND name NOT IN ('#itemsManageTable#','sqlite_sequence')";
        sqlInsertItemValue = "INSERT OR IGNORE INTO #tableName# (TIME, VALUE) VALUES( #tablePrimaryValue#, CAST( ? as #dbType#) )";
    }

    /**
     * INFO: http://www.java2s.com/Code/Java/Database-SQL-JDBC/StandardSQLDataTypeswithTheirJavaEquivalents.htm
     */
    private void initSqlTypes() {
        logger.debug("JDBC::initSqlTypes: Initialize the type array");
        sqlTypes.put("tablePrimaryValue", "strftime(" + DATETIME_FORMAT + " , 'now', 'localtime')");
    }

    /**
     * INFO: https://github.com/brettwooldridge/HikariCP
     */
    private void initDbProps() {
        // Properties for HikariCP
        databaseProps.setProperty("driverClassName", DRIVER_CLASS_NAME);
        // driverClassName OR BETTER USE dataSourceClassName
        // databaseProps.setProperty("dataSourceClassName", DATA_SOURCE_CLASS_NAME);
    }

    /**************
     * ITEMS DAOs *
     **************/

    @Override
    public @Nullable String doGetDB() throws JdbcSQLException {
        try {
            return Yank.queryColumn(sqlGetDB, "file", String.class, null).get(0);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
    }

    @Override
    public ItemsVO doCreateItemsTableIfNot(ItemsVO vo) throws JdbcSQLException {
        String sql = StringUtilsExt.replaceArrayMerge(sqlCreateItemsTableIfNot,
                new String[] { "#itemsManageTable#", "#colname#", "#coltype#" },
                new String[] { formattedIdentifier(vo.getItemsManageTable()), vo.getColname(), vo.getColtype() });
        logger.debug("JDBC::doCreateItemsTableIfNot sql={}", sql);
        try {
            Yank.execute(sql, null);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
        return vo;
    }

    /*************
     * ITEM DAOs *
     *************/
    @Override
    public void doStoreItemValue(Item item, State itemState, ItemVO vo) throws JdbcSQLException {
        ItemVO storedVO = storeItemValueProvider(item, itemState, vo);
        String sql = StringUtilsExt.replaceArrayMerge(sqlInsertItemValue,
                new String[] { "#tableName#", "#dbType#", "#tablePrimaryValue#" },
                new String[] { formattedIdentifier(storedVO.getTableName()), storedVO.getDbType(),
                        sqlTypes.get("tablePrimaryValue") });
        Object[] params = { storedVO.getValue() };
        logger.debug("JDBC::doStoreItemValue sql={} value='{}'", sql, storedVO.getValue());
        try {
            Yank.execute(sql, params);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
    }

    @Override
    public void doStoreItemValue(Item item, State itemState, ItemVO vo, ZonedDateTime date) throws JdbcSQLException {
        ItemVO storedVO = storeItemValueProvider(item, itemState, vo);
        String sql = StringUtilsExt.replaceArrayMerge(sqlInsertItemValue,
                new String[] { "#tableName#", "#dbType#", "#tablePrimaryValue#" },
                new String[] { formattedIdentifier(storedVO.getTableName()), storedVO.getDbType(),
                        "strftime(" + DATETIME_FORMAT + " , ?, 'unixepoch', 'localtime')" });

        double epochSecondsWithMillis = date.toInstant().toEpochMilli() / 1_000.0;
        Object[] params = { epochSecondsWithMillis, storedVO.getValue() };
        logger.debug("JDBC::doStoreItemValue sql={} epochSecondsWithMillis={} value='{}'", sql, epochSecondsWithMillis,
                storedVO.getValue());
        try {
            Yank.execute(sql, params);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
    }

    /****************************
     * SQL generation Providers *
     ****************************/

    /*****************
     * H E L P E R S *
     *****************/

    /******************************
     * public Getters and Setters *
     ******************************/
}
