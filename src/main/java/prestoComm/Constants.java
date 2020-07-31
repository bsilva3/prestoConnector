package prestoComm;

import java.io.File;

public class Constants {

    //Presto main dir location Static for now
    //TODO: add a config file that shows the location of presto instalation
    public static final String PRESTO_DIR = "/home/bruno/Desktop/presto-server-330";
    public static final String PRESTO_BIN = PRESTO_DIR + File.separator + "bin";
    public static final String PRESTO_PROPERTIES_FOLDER = PRESTO_DIR + File.separator + "etc" + File.separator + "catalog"+File.separator;

    //files
    public static final String FILES_DIR = "files";
    public static final String IMAGES_DIR= FILES_DIR+ File.separator+"images"+File.separator;
    public static final String DATATYPE_CONVERTIBLES_DICT = FILES_DIR + File.separator + "convertible_datatypes.csv";
    public static final String LOADING_GIF = IMAGES_DIR + File.separator + "loading.gif";

    //SQLITE
    public static final String SQLITE_DB = "metadataBD";
    public static final String SQLITE_DB_FOLDER = "metadata";
    // ---------tables and columns for SQLITE--------------

    public static final String ID_FIELD = "id";

    //Table to store info about types of databases
    public static final String DB_TYPE_DATA = "db_type_data";
    //Columns
    public static final String DB_TYPE_NAME_FIELD = "db_name";
    public static final String DB_TYPE_MODEL_FIELD = "db_model_type";
    public static final String DB_TYPE_QUERY_FIELD = "catalog_access_query";


    //Table for the database data storage
    public static final String DB_DATA = "db_data";
    //columns
    public static final String DB_DATA_NAME_FIELD = "name";
    public static final String DB_DATA_SERVER_FIELD = "server_url";
    public static final String DB_DATA_USER_FIELD = "user";
    public static final String DB_DATA_PASS_FIELD = "pass";
    public static final String DB_DATA_TYPE_ID_FIELD = "db_type_id";


    //table for the table data storage
    public static final String TABLE_DATA = "table_data";
    //columns
    public static final String TABLE_DATA_NAME_FIELD = "name";
    public static final String TABLE_DATA_SCHEMA_NAME_FIELD = "schema_name";
    public static final String TABLE_DATA_DB_ID_FIELD = "db_id";

    //table for the column data storage
    public static final String COLUMN_DATA = "column_data";
    //columns in this table
    public static final String COLUMN_DATA_NAME_FIELD = "name";
    public static final String COLUMN_DATA_TYPE_FIELD = "data_type";
    public static final String COLUMN_DATA_IS_PRIMARY_KEY_FIELD = "is_primary_key";
    public static final String COLUMN_DATA_FOREIGN_KEY_FIELD = "foreign_key_reference";
    public static final String COLUMN_DATA_TABLE_RELATION_FIELD = "table_relation";
    public static final String COLUMN_DATA_TABLE_FIELD = "table_id";

    // ---------- global tables
    //table for the global table data storage
    public static final String GLOBAL_TABLE_DATA = "global_table_data";
    //columns in this table
    public static final String GLOBAL_TABLE_DATA_NAME_FIELD = "name";
    public static final String GLOBAL_TABLE_DATA_ID_FIELD = "global_table_id";

    //table for the global column data storage
    public static final String GLOBAL_COLUMN_DATA = "global_column_data";
    //columns in this table
    public static final String GLOBAL_COLUMN_DATA_NAME_FIELD = "name";//name of the global table
    public static final String GLOBAL_COLUMN_DATA_TABLE_FIELD = "global_table_id";//table the column belongs to
    public static final String GLOBAL_COLUMN_DATA_ID_FIELD = "global_col_id";
    public static final String GLOBAL_COLUMN_DATA_TYPE_FIELD = "data_type";
    public static final String GLOBAL_COLUMN_DATA_PRIMARY_KEY_FIELD = "is_primary_key";
    public static final String GLOBAL_COLUMN_DATA_FOREIGN_KEY_FIELD = "foreign_key";

    //table for the correspondences between global columns and local columns. 1 local column belongs to 1 global column in 1 cube. 1 Global column may contain
    //multiple local columns (because they matched)
    public static final String CORRESPONDENCES_DATA = "global_local_correspondences";
    //columns in this table
    public static final String CORRESPONDENCES_GLOBAL_COL_FIELD = "global_col_id";
    public static final String CORRESPONDENCES_LOCAL_COL_FIELD = "local_col_id";
    public static final String CORRESPONDENCES_CONVERSION_FIELD = "conversion"; //the necessary conversion from global to local
    public static final String CORRESPONDENCES_TYPE_FIELD = "conversion_type";
    
    // Tables for mapping of global tables to star schema -----------------
    //multidimensional table
    public static final String MULTIDIM_TABLE = "multidimensional_table";
    //contains cube id and global table id
    public static final String MULTIDIM_TABLE_TYPE = "multidimensional_type";
    public static final String MULTIDIM_TABLE_ID = "multidim_table_id";
    public static final String MULTIDIM_TABLE_ISFACTS = "isFacts";
    public static final String MULTIDIM_TABLE_CUBE_ID = "cube_id";

    //multidimensional column
    public static final String MULTIDIM_COLUMN = "multidimensional_column";
    //contains multidim id and global column id
    public static final String MULTIDIM_COLUMN_TYPE = "multidimensional_type";
    public static final String MULTIDIM_COL_GLOBAL_COLUMN_ID = "glob_col_id";
    public static final String MULTIDIM_COLUMN_MEASURE = "isMeasure";

    //query save
    public static final String QUERY_SAVE = "query_save";
    public static final String QUERY_ID = "query_id";
    public static final String QUERY_NAME = "query_name";
    public static final String QUERY_CUBE_ID = "cube_id";
    //query save: rows
    public static final String QUERY_ROW = "query_row";
    public static final String QUERY_GLOBAL_TABLE_ID = "global_table_id";//table the column belongs to
    public static final String QUERY_GLOBAL_COLUMN_ID = "global_column_id";
    public static final String QUERY_ORDER_BY = "query_orderby";

    //query save: columns
    public static final String QUERY_COLS = "query_columns";

    //query save: measures
    public static final String QUERY_MEASURES = "query_measures";
    public static final String QUERY_AGGR_OP = "operation";

    //query save: filters
    public static final String QUERY_FILTERS = "query_filters";
    public static final String QUERY_FILTERS_STR = "filters_str";

    public static final String GLOBAL_TABLE_DATA_MULTI_TYPE_FIELD = "multidimensional_type";
    public static final String CUBE_TABLE = "cube";
    //columns in this table
    public static final String CUBE_NAME = "name";
    public static final String CUBE_ID_FIELD = "cube_id";
    public static final String CUBE_TYPE_FIELD = "cube_type";

    //Presto SHOW COLUMNS FROM column names of query result
    public static final String SHOW_COLS_COLUMN = "Column";
    public static final String SHOW_COLS_TYPE = "Type";

    //Schema name that contains the views with info regarding table constraints
    public static final String METADATA_VIEW_SCHEMA_NAME = "constraint_data_schema";
    //View name that must be created in each database to get information about foreign key relations
    public static final String METADATA_VIEW_FOREIGN_KEY_NAME = "foreign_keys_relations";
    public static final String METADATA_VIEW_SCHEMA = "TABLE_SCHEMA";
    public static final String METADATA_VIEW_TABLE = "TABLE_NAME";
    public static final String METADATA_VIEW_COLUMN = "COLUMN_NAME";
    public static final String METADATA_VIEW_REFERENCED_SCHEMA = "REFERENCED_TABLE_SCHEMA";
    public static final String METADATA_VIEW_REFERENCED_TABLE = "REFERENCED_TABLE_NAME";
    public static final String METADATA_VIEW_REFERENCED_COLUMN = "REFERENCED_COLUMN_NAME";

    public static final String METADATA_VIEW_PRIMARY_KEY_NAME = "primary_keys";
    public static final String METADATA_VIEW_PRIMARY_SCHEMA = "TABLE_SCHEMA";
    public static final String METADATA_VIEW_PRIMARY_TABLE = "TABLE_NAME";
    public static final String METADATA_VIEW_PRIMARY_COLUMN = "COLUMN_NAME";


    //messages and errors

    public static final String SUCCESS_STR = "Success";



    //SCHEMA exclusions that are database characteristic and have no interest in a business analytics context
    public static final String[] PSQL_SCHEMA_EXCL = new String[] {"schema_info", "pg_catalog"};
    public static final String[] MYSQL_SCHEMA_EXCL = new String[] {"schema_info", "pg_catalog"};
    public static final String[] SQLSERVER_SCHEMA_EXCL = new String[] {"schema_info", "pg_catalog"};
    public static final String[] MONGODB_SCHEMA_EXCL = new String[] {"admin"};

}
