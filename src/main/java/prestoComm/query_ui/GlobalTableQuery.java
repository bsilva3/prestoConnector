package prestoComm.query_ui;

import helper_classes.*;
import prestoComm.PrestoMediator;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class GlobalTableQuery {

    private Map<GlobalTableData, List<GlobalColumnData>> selectRows;
    private Map<GlobalTableData, List<GlobalColumnData>> selectColumns;
    private List<String> measures;
    private List<String> orderBy;
    private String filterQuery;
    private FactsTable factsTable;
    private PrestoMediator presto;

    private List<List<String>> pivotValues;

    private final static int LIMIT_NUMBER = 50000;

    public GlobalTableQuery(PrestoMediator presto, FactsTable factsTable) {
        this.presto = presto;
        this.factsTable = factsTable;
        selectRows = new HashMap<>();
        selectColumns = new HashMap<>();
        measures = new ArrayList<>();
        orderBy = new ArrayList<>();
        pivotValues = new ArrayList<>();
    }

    public void addOrderByRow(String groupByRow){
        orderBy.add(groupByRow);
    }

    public void addSelectColumn(GlobalTableData table, GlobalColumnData col){
        //table already present, add the column
        if (selectColumns.containsKey(table)){
            List<GlobalColumnData> listCols = selectRows.get(table);
            if (!listCols.contains(col)){
                selectColumns.get(table).add(col);
                return;
            }
        }
        else{
            List <GlobalColumnData> l = new ArrayList<>();
            l.add(col);
            selectColumns.put(table, l);
        }
    }

    public void addSelectRow(GlobalTableData table, GlobalColumnData col){
        //table already present, add the column
        if (selectRows.containsKey(table)){
            List<GlobalColumnData> listCols = selectRows.get(table);
            if (!listCols.contains(col)){
                selectRows.get(table).add(col);
                return;
            }
        }
        else{
            List <GlobalColumnData> l = new ArrayList<>();
            l.add(col);
            selectRows.put(table, l);
        }
    }

    public void addMeasure(String measure){
        this.measures.add(measure);
    }

    public boolean deleteSelectColumnFromTable(GlobalTableData table, GlobalColumnData columnName){
        boolean success = selectColumns.get(table).remove(columnName);
        if (success && selectColumns.get(table).size()==0){
            selectColumns.remove(table);
        }
        return success;
    }

    public boolean deleteSelectRowFromTable(GlobalTableData table, GlobalColumnData columnName){
        boolean success = selectRows.get(table).remove(columnName);
        if (success && selectRows.get(table).size()==0){
            selectRows.remove(table);
        }
        removeOrderByIfPresent(table.getTableName()+"."+columnName.getName());
        return success;
    }

    /**
     * Removes an order by element if present. Searches for strings in the form 'tablename.columnname'
     * @param orderByElem
     */
    public void removeOrderByIfPresent(String orderByElem){
        for (String s : orderBy){
            if (s.contains(orderByElem)){
                orderBy.remove(s);
                return;
            }
        }
    }

    public void removeMeasure(String measure){
        this.measures.remove(measure);
    }

    public String getLocalTableQuery(GlobalTableData t, List<GlobalColumnData> selectCols){
        MappingType mapping = t.getMappingType();
        if (mapping == MappingType.Simple)
            return handleSimpleMapping(t, selectCols);
        else if (mapping == MappingType.Horizontal)
            return handleHorizontalMapping(t, selectCols);
        else if (mapping == MappingType.Vertical)
            return handleVerticalMapping(t, selectCols);
        else
            return "Error: Invalid Mapping Type";
    }

    public String getLocalTableQuery(GlobalTableData t){
        MappingType mapping = t.getMappingType();
        if (mapping == MappingType.Simple)
            return handleSimpleMapping(t);
        else if (mapping == MappingType.Horizontal)
            return handleHorizontalMapping(t);
        else if (mapping == MappingType.Vertical)
            return handleVerticalMapping(t, t.getGlobalColumnDataList());
        else
            return "Error: Invalid Mapping Type";
    }

    //Creates a SELECT XXX FROM () with the necessary inner queries to get local schema data
    public String buildQuerySelectRowsOnly(){
        String query = "SELECT ";
        //first add to the select the dimensions columns

        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> dimTable : selectRows.entrySet()){
            List<GlobalColumnData> cols = dimTable.getValue();
            GlobalTableData t = dimTable.getKey();
            for (GlobalColumnData c : cols){
                query+= t.getTableName()+"."+c.getName()+",";

            }
        }

        query = query.substring(0, query.length() - 1);//last elemment without comma
        query+= " FROM ";
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> tableSelectRows : selectRows.entrySet()){
            //for each global table
            GlobalTableData t = tableSelectRows.getKey();
            List<GlobalColumnData> rowsForSelect = tableSelectRows.getValue();
            //query+= "FROM (";
            String subQueries = "("+getLocalTableQuery(t, rowsForSelect);

            if(subQueries.contains("Error")){
                return subQueries;//propagate error
            }
            query+=subQueries;
            query+= ") AS "+t.getTableName()+",";
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma

        if (filterQuery.length() > 0){
            query += " WHERE " + filterQuery;//add filters to the query (if there are filters). Filters will be applied to the outer query
        }

        return query;
    }

    //Creates a 'SELECT XXX FROM ( ) join fatcs with dims foreign keys' with the necessary inner queries to get local schema data and join facts foreign keys with dims. Also
    //performs aggregations on the measures and groups the dimensions rows
    public String buildQuerySelectRowsAndMeasures(boolean selectMeasure) {
        return buildQuerySelectRowsAndMeasures("SELECT ", selectMeasure);
    }

    public String buildQuerySelectRowsAndMeasures(String queryBegin, boolean selectMeasure) {
        String query = queryBegin;
        Map<GlobalTableData, List<GlobalColumnData>> tableSelectRowsWithPrimKeys = new HashMap<>();
        //first add to the select the dimensions columns
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> dimTable : selectRows.entrySet()){
            List<GlobalColumnData> cols = dimTable.getValue();
            GlobalTableData t = dimTable.getKey();
            GlobalTableData newt = new GlobalTableData(t.getTableName());
            List<GlobalColumnData> newCols = new ArrayList<>();
            boolean primKeyIsSelected = false;
            for (GlobalColumnData c : cols){
                query+= t.getTableName()+"."+c.getName()+",";
                GlobalColumnData newC = new GlobalColumnData(c.getName(), c.getDataType(), c.isPrimaryKey(), c.getLocalColumns());
                newCols.add(newC);
                if (c.isPrimaryKey()) {
                    primKeyIsSelected = true;
                }
            }
            if (!primKeyIsSelected){
                List<GlobalColumnData> cps = dimTable.getKey().getPrimaryKeyColumns(); //primary key column missing
                for (GlobalColumnData cp : cps)
                    newCols.add(new GlobalColumnData(cp.getName(), cp.getDataType(), cp.isPrimaryKey(), cp.getLocalColumns()));
            }
            newt.setGlobalColumnData(newCols);
            tableSelectRowsWithPrimKeys.put(newt, newCols);
        }

        if (selectMeasure) {
            //add to the select the measures with the aggregation operation (in the form 'aggr(measureName)'). This a string taken from the drop area in the interface.
            for (String measureCol : measures) {
                String measureName = measureCol.split("[()]")[1]; //split on first space to the measure name (its in the form "aggr(measureName)" )
                query += measureCol + " AS " + measureName + ",";
            }
        }

        query = query.substring(0, query.length() - 1);//last elemment without comma
        query+= " FROM ";

        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> tableSelectRows : tableSelectRowsWithPrimKeys.entrySet()){
            //for each global column create inner queries in the 'From' clause
            GlobalTableData t = tableSelectRows.getKey();
            List<GlobalColumnData> rowsForSelect = tableSelectRows.getValue();

            String subQueries = getLocalTableQuery(t, rowsForSelect);

            if(subQueries.contains("Error")){
                return subQueries;//propagate error
            }
            query+="("+subQueries;
            //Join on facts table
            query+= ") AS " + t.getTableName()+",";
        }
        //get facts table in from clause
        String subQueries = "("+getLocalTableQuery(factsTable.getGlobalTable());
        if(subQueries.contains("Error")){
            return subQueries;//propagate error
        }
        query+=subQueries;
        query+= ") AS "+factsTable.getGlobalTable().getTableName()+" ";

        //perform Where on facts foreign key fields = dimensions referenced primary keys
        query+= " WHERE (";

        Map<GlobalColumnData, Boolean> factColumns = factsTable.getColumns();
        //foreign key of facts = prim key of the dimensions
        for (Map.Entry<GlobalColumnData, Boolean> factColumn : factColumns.entrySet()) {
            boolean isMeasure = factColumn.getValue();
            if (!isMeasure) { //it must be a foreign key, check if it references a dim table
                for (GlobalTableData tableDim : tableSelectRowsWithPrimKeys.keySet()) {

                    GlobalColumnData factsCol = factColumn.getKey();
                    if (factsCol.hasForeignKey()) {
                        GlobalColumnData referencedCol = isFactsColReferencingDimTable(factsCol.getForeignKey(), tableDim);
                        if (referencedCol != null) {
                            query += tableDim.getTableName() + "." + referencedCol.getName() + " = " + factsTable.getGlobalTable().getTableName() + "." + factsCol.getName();
                            query += " AND ";
                            break;
                        }
                    }
                }
            }
        }

        query = query.substring(0, query.length() - "AND ".length());//last column is whithout a comma
        query+=")"; //close where clause

        //aditional filters set by user here
        if (filterQuery.length() > 0){
            query += " AND ( " + filterQuery +")";//add filters to the query (if there are filters). Filters will be applied to the outer query
        }

        //groupby for each dimension column (only if aggregation operation is made)
        query += " GROUP BY ( ";
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> tableSelectRows : selectRows.entrySet()) {
            //for each global column create inner queries in the 'From' clause
            GlobalTableData table = tableSelectRows.getKey();
            List<GlobalColumnData> columns = tableSelectRows.getValue();
            for (GlobalColumnData col : columns) {
                query +=table.getTableName()+"."+col.getName()+",";
            }
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma
        query += ")"; //close group by

        return query;
    }

    //Creates a 'SELECT XXX FROM ( ) join fatcs with dims foreign keys' with the necessary inner queries to get local schema data and join facts foreign keys with dims. Also
    //performs aggregations on the measures and groups the dimensions rows
    /*public String buildQuerySelectRowsAndMeasures(){
        String query = "SELECT ";
        Collection<List<GlobalColumnData>> dimsRows = (Collection<List<GlobalColumnData>>) selectRows.values(); //list with a list of rows of each dim table
        //first add to the select the dimensions columns
        for (List<GlobalColumnData> dimsRowsOfTable : dimsRows){
            for (GlobalColumnData c : dimsRowsOfTable){
                query+= c.getName()+",";
            }
        }
        //add to the select the measures with the aggregation operation (in the form 'aggr(measureName)'). This a string taken from the drop are in the interface.
        for (String measureCol : measures){
            String measureName = measureCol.split("[()]")[1]; //split on first space to the measure name (its in the form "aggr(measureName)" )
            query+= measureCol+" AS "+measureName+",";
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma
        query+= " FROM ";
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> tableSelectRows : selectRows.entrySet()){
            //for each global column create inner queries in the 'From' clause
            GlobalTableData t = tableSelectRows.getKey();
            List<GlobalColumnData> rowsForSelect = tableSelectRows.getValue();

            String subQueries = getLocalTableQuery(t, rowsForSelect);

            if(subQueries.contains("Error")){
                return subQueries;//propagate error
            }
            query+="("+subQueries;
            //Join on facts table
            query+= ") AS " + t.getTableName()+",";
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma

        Map<GlobalColumnData, Boolean> factColumns = factsTable.getColumns();
        String factsLocalTableQuery = getLocalTableQuery(factsTable.getGlobalTable()); //not efficient (repeats for every join..)
        if (factsLocalTableQuery.contains("Error")){
            return factsLocalTableQuery;
        }
        //foreign key of facts = prim key of the dimensions
        for (Map.Entry<GlobalColumnData, Boolean> factColumn : factColumns.entrySet()){
            boolean isMeasure = factColumn.getValue();
            if (!isMeasure){ //it must be a foreign key, check if it references a dim table
                for (GlobalTableData tableDim : selectRows.keySet()){

                    GlobalColumnData factsCol = factColumn.getKey();
                    if (factsCol.hasForeignKey()){
                        GlobalColumnData referencedCol = isFactsColReferencingDimTable(factsCol.getForeignKey(), tableDim);
                        if (referencedCol != null){
                            query+= " JOIN (" + factsLocalTableQuery +") AS "+factsTable.getGlobalTable().getTableName()+" ON " ;
                            query+= tableDim.getTableName()+"."+referencedCol.getName() +" = "+factsTable.getGlobalTable().getTableName()+"."+factsCol.getName();
                            break;
                        }
                    }
                }
            }
        }
        //validate query so far
        if (!query.contains("JOIN"))
            return "Error: Query not correctly formulated";
        //group by for each dim column
        query += " GROUP BY ( ";
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> tableSelectRows : selectRows.entrySet()) {
            //for each global column create inner queries in the 'From' clause
            GlobalTableData table = tableSelectRows.getKey();
            List<GlobalColumnData> columns = tableSelectRows.getValue();
            for (GlobalColumnData col : columns) {
                query +=table.getTableName()+"."+col.getName()+",";
            }
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma
        query += ")"; //close group by

        //Oder by here if (any)...
        return query;
    }*/

    public String buildQuerySelectRowsColsAndMeasures() {

        String query = "SELECT ";

        //get distinct values of columns. If multiple columns, get all distinct combinations
        List<List<String>> valuesByGlobalCol = getAllDifferentValuesOfColumn();
        //this.pivotValues = valuesByGlobalCol;

        //add to select atributes in the 'rows' area
        Map<GlobalTableData, List<GlobalColumnData>> tableSelectRowsWithPrimKeys = new HashMap<>();
        //first add to the select the dimensions columns
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> dimTable : selectRows.entrySet()){
            List<GlobalColumnData> cols = dimTable.getValue();
            GlobalTableData t = dimTable.getKey();
            GlobalTableData newt = new GlobalTableData(t.getTableName());
            newt.setId(t.getId());
            List<GlobalColumnData> newCols = new ArrayList<>();
            boolean primKeyIsSelected = false;
            for (GlobalColumnData c : cols){
                query+= t.getTableName()+"."+c.getName()+",";
                GlobalColumnData newC = new GlobalColumnData(c.getName(), c.getDataType(), c.isPrimaryKey(), c.getLocalColumns());//new references so that the originals are unnaltered
                newCols.add(newC);
                if (c.isPrimaryKey()) {
                    primKeyIsSelected = true;
                }
            }

            if (!primKeyIsSelected){
                List<GlobalColumnData> cps = dimTable.getKey().getPrimaryKeyColumns(); //primary key column missing
                for (GlobalColumnData cp : cps)
                    newCols.add(new GlobalColumnData(cp.getName(), cp.getDataType(), cp.isPrimaryKey(), cp.getLocalColumns(), cp.getColumnID()));
            }
            newt.setGlobalColumnData(newCols);
            tableSelectRowsWithPrimKeys.put(newt, newCols);
        }
        query+=" ";
        //get ordered list of columns's fullnames:

        List<String> colNames = new ArrayList<>();
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> colSelect : selectColumns.entrySet()){
            GlobalTableData t = colSelect.getKey();
            List<GlobalColumnData> cs = colSelect.getValue();
            for (GlobalColumnData c : cs){
                colNames.add(t.getTableName()+"."+c.getName());
            }
        }
        if (colNames.size() != valuesByGlobalCol.get(0).size()){//list of columns of values must be same length as list of values of columns
            return "Error";
        }
        //clauses to create columns
        //for each measure, iterate
        for (String measure : measures){
            String measureName = getMeasureName(measure);
            String measureOP = getMeasureOP(measure);
            for (List<String> pairs : valuesByGlobalCol){//for each list of value of each column
                String valueColEnumeration = ""; //colName = value AND ColName = value etc...
                String valueAlias = ""; //as aliasName
                List<String> valuesRaw = new ArrayList<>();
                for (int i = 0; i < pairs.size(); i++) {//v is already escaped with ''
                    String v = pairs.get(i);
                    String valueRaw = ""; //value with no '
                    if (v.charAt(0)=='\'' && v.charAt(v.length()-1) == '\''){
                        valueRaw = v.substring(1, v.length()-1);//remove the ' at the beginning and end of string
                    }
                    else
                        valueRaw = v;
                    valueRaw = valueRaw.replaceAll("''", "'");//all ' were replaced by '' to be escaped when used as column anmes for presto. Convert back to single '
                    //valueRaw = v.replaceAll("'", "");
                    valuesRaw.add(valueRaw);
                    //if (valueRaw.isEmpty())
                        //valueRaw ="''";
                    valueColEnumeration += colNames.get(i) + " = "+v+" AND ";//building a colName = value AND ColName = value etc...
                    valueAlias += valueRaw + "_";
                }
                pivotValues.add(valuesRaw);
                //remove last AND from enumerations and last _ from alias
                valueColEnumeration = valueColEnumeration.substring(0, valueColEnumeration.length() - " AND ".length());//remove last AND from enumeration
                valueAlias = valueAlias.substring(0, valueAlias.length() - "_".length());//remove last _ from alias
                if (Utils.stringIsNumericOrBoolean(valueAlias)) {
                    valueAlias = "_" + valueAlias; //boolean or numerics are not accepted as column names
                }
                if (valueAlias.trim().isEmpty()){//empty alias name, add a new name
                    valueAlias="empty";
                }
                valueAlias = "\""+valueAlias+"\"";//space between chars in alias is not allowed, add double quotes
                if (measureOP.equalsIgnoreCase("COUNT")) //inneficient, if running for every row...
                    query+= " SUM(CASE WHEN "+ valueColEnumeration + " THEN 1 ELSE 0 END) AS "+valueAlias+", ";
                else if (measureOP.equalsIgnoreCase("SUM")) //inneficient, if running for every row...
                    query+= " SUM(CASE WHEN "+ valueColEnumeration + " THEN "+measureName +" ELSE 0 END) AS "+valueAlias+", ";
                else if (measureOP.equalsIgnoreCase("AVG")) //inneficient, if running for every row...
                    query+= " AVG(CASE WHEN "+ valueColEnumeration + " THEN "+measureName +" ELSE NULL END) AS "+valueAlias+", ";
                else if (measureOP.equalsIgnoreCase("SIMPLE")) //inneficient, if running for every row...
                    query+= " (CASE WHEN "+ valueColEnumeration + " THEN "+measureName +" ELSE 0 END) AS "+valueAlias+", ";
            }
        }

        query = query.substring(0, query.length() - ", ".length());//last elemment without comma
        query+= " FROM ";

        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> tableSelectRows : tableSelectRowsWithPrimKeys.entrySet()){
            //for each global column create inner queries in the 'From' clause
            GlobalTableData t = tableSelectRows.getKey();
            List<GlobalColumnData> rowsForSelect = tableSelectRows.getValue();
            if (selectColumns.containsKey(t)){
                List<GlobalColumnData> colsForSelect = selectColumns.get(t);
                for (GlobalColumnData colForSelect : colsForSelect) {
                    if (!rowsForSelect.contains(colForSelect)) { //in the subqueries, must also fetch the attributes in the 'columns' area
                        rowsForSelect.add(colForSelect);
                    }
                }
            }
            //todo must add in the from a inner query to fetch table of atributes in the 'columns' area IF such table is not already in the 'rows area'

            String subQueries = getLocalTableQuery(t, rowsForSelect);

            if(subQueries.contains("Error")){
                return subQueries;//propagate error
            }
            query+="("+subQueries;
            //Join on facts table
            query+= ") AS " + t.getTableName()+",";
        }
        //get facts table in from clause
        String subQueries = "("+getLocalTableQuery(factsTable.getGlobalTable());
        if(subQueries.contains("Error")){
            return subQueries;//propagate error
        }
        query+=subQueries;
        query+= ") AS "+factsTable.getGlobalTable().getTableName()+" ";

        //perform Where on facts foreign key fields = dimensions referenced primary keys
        query+= " WHERE (";

        Map<GlobalColumnData, Boolean> factColumns = factsTable.getColumns();
        //foreign key of facts = prim key of the dimensions
        for (Map.Entry<GlobalColumnData, Boolean> factColumn : factColumns.entrySet()) {
            boolean isMeasure = factColumn.getValue();
            if (!isMeasure) { //it must be a foreign key, check if it references a dim table
                for (GlobalTableData tableDim : tableSelectRowsWithPrimKeys.keySet()) {

                    GlobalColumnData factsCol = factColumn.getKey();
                    if (factsCol.hasForeignKey()) {
                        GlobalColumnData referencedCol = isFactsColReferencingDimTable(factsCol.getForeignKey(), tableDim);
                        if (referencedCol != null) {
                            query += tableDim.getTableName() + "." + referencedCol.getName() + " = " + factsTable.getGlobalTable().getTableName() + "." + factsCol.getName();
                            query += " AND ";
                            break;
                        }
                    }
                }
            }
        }

        query = query.substring(0, query.length() - "AND ".length());//last column is whithout a comma
        query+=")"; //close where clause

        //aditional filters set by user here
        if (filterQuery.length() > 0){
            query += " AND ( " + filterQuery +")";//add filters to the query (if there are filters). Filters will be applied to the outer query
        }

        //groupby for each dimension column (only if aggregation operation is made)
        query += " GROUP BY ( ";
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> tableSelectRows : selectRows.entrySet()) {
            //for each global column create inner queries in the 'From' clause
            GlobalTableData table = tableSelectRows.getKey();
            List<GlobalColumnData> columns = tableSelectRows.getValue();
            for (GlobalColumnData col : columns) {
                query +=table.getTableName()+"."+col.getName()+",";
            }
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma
        query += ")"; //close group by

        return query;
    }


    public List<List<String>>  getAllDifferentValuesOfColumn(){
        List<GlobalColumnData> allCols = new ArrayList<>();
        List<GlobalTableData> allTables = new ArrayList<>();
        for (Map.Entry<GlobalTableData, List<GlobalColumnData>> colSelect : selectColumns.entrySet()){
            GlobalTableData t = colSelect.getKey();
            allTables.add(t);
            List<GlobalColumnData> cs = colSelect.getValue();
            for (GlobalColumnData c : cs){
                c.setFullName(t.getTableName()+"."+c.getName());
                allCols.add(c);
            }
        }
        String query = "SELECT DISTINCT (";
        for (GlobalColumnData c : allCols){
            query+=c.getFullName()+",";
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma
        query+=") FROM ";
        for (GlobalTableData t : allTables){
            String subQueries = getLocalTableQuery(t, selectColumns.get(t));

            if(subQueries.contains("Error")){
                return null;//propagate error
            }
            query+="("+subQueries;
            //Join on facts table
            query+= ") AS " + t.getTableName()+",";
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma
        query+=" ORDER BY (";
        for (GlobalColumnData c : allCols){
            query+=c.getFullName()+",";
        }
        query = query.substring(0, query.length() - 1);//last elemment without comma
        query+=")";
        System.out.println("value query: "+query);
        ResultSet results = presto.getLocalTablesQueries(query);
        List<List<String>> values = new ArrayList<>();//tablename.columnname; List of values
        try {
            //results.beforeFirst(); //return to begining
            ResultSetMetaData rsmd = results.getMetaData();
            while(results.next()){
                //Fetch each rows from the ResultSet, and add to ArrayList of different values
                // ResultSet column indices start at 1
                List<String> valuePairs = new ArrayList<>();
                if (allCols.size() == 1) {
                    String value = results.getString(1);
                    if (!Utils.stringIsNumericOrBoolean(value))
                        value = "'" + value.replaceAll("'", "''") + "'";//2 single quotes to escape any single quotes in the string ('Women's dress' -> ''Women''s dress'
                    valuePairs.add(value);
                    values.add(valuePairs);//add list of values
                }
                else if (allCols.size() > 1){
                    Map<String, String> valueFields = (Map<String, String>) results.getObject(1);//returns only one columns with a map with fieldx=value, fieldy=value...
                    for (Map.Entry<String, String> valueField : valueFields.entrySet()){//mapp with key=fieldName, value=value from select
                        String value = valueField.getValue();
                        if (!Utils.stringIsNumericOrBoolean(value))
                            value = "'"+value.replaceAll("'", "''")+"'";//2 single quotes to escape any single quotes in the string ('Women's dress' -> ''Women''s dress'
                        valuePairs.add(value);
                    }
                    values.add(valuePairs);//add the list of pair values.
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return values;
    }

    private String getMeasureName(String measureAndOP){
        return measureAndOP.split("[()]")[1]; //split on first space to the measure name (its in the form "aggr(measureName)" )
    }
    private String getMeasureOP(String measureAndOP){
        return measureAndOP.split("[\\(]")[0]; //split on first space to the measure name (its in the form "aggr(measureName)" )
    }

    public String buildQuery(){
        this.pivotValues.clear();//reset elements
        String query = "";
        if (selectColumns.size() == 0 && measures.size() == 0 && selectRows.size() > 0){
            query = buildQuerySelectRowsOnly();
        }
        else if (selectColumns.size() == 0 && measures.size() > 0 && selectRows.size() > 0){
            query = buildQuerySelectRowsAndMeasures(true);
        }
        else if (selectColumns.size() > 0 && measures.size() > 0 && selectRows.size() > 0){
            query = buildQuerySelectRowsColsAndMeasures();
        }
        else
            return "Error invalid query elements given";
        //add order by elements if any are selected
        if (orderBy.size() > 0){
            query += " ORDER BY ";
            for (String s : orderBy){
                query+=s +",";
            }
            query = query.substring(0, query.length() - 1);//last elemment without comma
        }
        //Add limit of max lines
        query += " LIMIT "+LIMIT_NUMBER;
        return query;
    }

    private List<GlobalColumnData> getSelectedMeasureCols(List<String> measuresString){
        List<GlobalColumnData> selectedMeasures = new ArrayList<>();
        Set<GlobalColumnData> measureCols = factsTable.getColumns().keySet();
        for (String measureString : measuresString){
            String measureName = measureString.split("[()]")[1]; //split on first space to the measure name (its in the form "aggr(measureName)" )
            for (GlobalColumnData measureCol : measureCols){
                if (measureCol.getName().equals(measureName)){
                    selectedMeasures.add(measureCol);
                }
            }
        }
        return selectedMeasures;
    }

    private GlobalColumnData isFactsColReferencingDimTable(String foreignKey, GlobalTableData dimTable){
        String[] splitStr = foreignKey.split("\\.");
        String tableName = splitStr[0];
        String columnName = splitStr[1];
        if (dimTable.getTableName().equals(tableName)){
            List<GlobalColumnData> globalColsDims = dimTable.getGlobalColumnDataList();
            for (GlobalColumnData c : globalColsDims){
                if (c.getName().equals(columnName))
                    return c;
            }
        }
        return null;
    }

    private String handleSimpleMapping(GlobalTableData t, List<GlobalColumnData> selectCols){
        //for each local table that matches with this global table
        String query = " SELECT ";
        Set<TableData> localTables = t.getAllLocalTablesFromCols(selectCols);
        for (TableData localTable : localTables){
            List<ColumnData> localCols = localTable.getColumnsList();
            for (int i = 0; i < localCols.size()-1; i++){
                query+=localCols.get(i).getCompletePrestoColumnName() +", ";
            }
            query+=localCols.get(localCols.size()-1).getCompletePrestoColumnName()+" ";//last column is whithout a comma
            query+= "FROM "+localTable.getCompletePrestoTableName()+" ";
        }
        return query;
    }
    private String handleSimpleMapping(GlobalTableData t){
        //for each local table that matches with this global table
        String query = " SELECT * ";
        Set<TableData> localTables = t.getAllLocalTablesFromCols(t.getGlobalColumnDataList());
        for (TableData localTable : localTables){
            query+= "FROM "+localTable.getCompletePrestoTableName()+" ";
        }
        return query;
    }

    private String handleHorizontalMapping(GlobalTableData t, List<GlobalColumnData> selectCols){
        String query ="SELECT ";
        String tableUnionString = "UNION SELECT ";
        //for each local table that matches with this global table
        Set<TableData> localTables = t.getAllLocalTablesFromCols(selectCols);
        List<ColumnData> localCols = localTables.iterator().next().getColumnsList();
        List<String> localColNames = new ArrayList<>();
        for (ColumnData c : localCols){
            localColNames.add(c.getName());
        }
        for (TableData localTable : localTables){
            /*List<ColumnData> localCols = localTable.getColumnsList();
            for (int i = 0; i < localCols.size()-1; i++){
                query+=localCols.get(i).getCompletePrestoColumnName() +", ";
            }
            query+=localCols.get(localCols.size()-1).getCompletePrestoColumnName()+" ";//last column is whithout a comma*/
            for (String colName : localColNames){
                query+=localTable.getCompletePrestoTableName()+"."+colName +", ";
            }
            query = query.substring(0, query.length() - ", ".length());
            query+= " FROM "+localTable.getCompletePrestoTableName()+" ";
            query+=tableUnionString;
        }
        if (query.endsWith(tableUnionString)) {
            return query.substring(0, query.length() - tableUnionString.length());
        }
        return query;
    }

    private String handleHorizontalMapping(GlobalTableData t){
        String query ="SELECT * ";
        String tableUnionString = "UNION SELECT ";
        //for each local table that matches with this global table
        Set<TableData> localTables = t.getAllLocalTablesFromCols(t.getGlobalColumnDataList());
        for (TableData localTable : localTables){
            query+= "FROM "+localTable.getCompletePrestoTableName()+" ";
            query+=tableUnionString;
        }
        if (query.endsWith(tableUnionString)) {
            return query.substring(0, query.length() - tableUnionString.length());
        }
        return query;
    }

    private String handleVerticalMapping(GlobalTableData t, List<GlobalColumnData> selectCols) {
        //for each local table that matches with this global table
        String query = " SELECT ";
        String tableJoinString = "INNER JOIN ";
        List<GlobalColumnData> primKeyCols = t.getPrimaryKeyColumns();
        Map<GlobalColumnData, Set<ColumnData>> pkWithLocalCols = new HashMap<>();
        for (GlobalColumnData pk : primKeyCols){
            pkWithLocalCols.put(pk, pk.getLocalColumns());
        }
        //get all local tables WITH ONLY LOCAL COLS SELECTED BY USER for the select clause
        Set<TableData> localTablesFromSelectedCols = t.getLocalTablesFromColsVerticalMap(selectCols);//contains the tables in the inner query for select clause
        //select clause
        for (TableData localTable : localTablesFromSelectedCols){
            Set<ColumnData> localCols = new HashSet<>(localTable.getColumnsList());
            for (ColumnData col: localCols){
                query+=col.getCompletePrestoColumnName() +", ";
            }
        }
        if (query.endsWith(", ")) {
            query = query.substring(0, query.length() - ", ".length());//last column is whithout a comma
        }
        //if only one table on the correspondences of the selected global columns, no need to perform joins)
        if (localTablesFromSelectedCols.size() == 1) {
            query += " FROM " + localTablesFromSelectedCols.iterator().next().getCompletePrestoTableName();
        }
        else{
            //columns were selected that correspond to more than one local table, apply joins
            Set<TableData> localTablesComplete = t.getAllLocalTablesFromCols(t.getGlobalColumnDataList());//tables that have columns mappig to the selected cols. The local tables have all columns
            TableData originalTable = null;
            for (TableData tb : localTablesComplete){
                if (!tb.hasForeignKeys())
                    originalTable = tb;
            }
            if (originalTable == null)
                return null; //there MUST be a table with no foreign keys (the original table)
            localTablesComplete.remove(originalTable);
            query += " FROM " + originalTable.getCompletePrestoTableName();
            List<ColumnData> pkOriginalColumns = originalTable.getPrimaryKeyColumns();
            Set<TableData> localTablesSelected = getAllDiferentLocalTablesInGlobalColumns(selectCols);//tables with complete columns, but only those that have cols mapped to the selected global cols
            localTablesSelected.remove(originalTable);
            for (TableData tb : localTablesSelected) {
                query += " "+tableJoinString + " " + tb.getCompletePrestoTableName();
                List<ColumnData> pkColumns = tb.getPrimaryKeyColumns();
                for (ColumnData pk : pkColumns){
                    ColumnData fk = getForeignKeyRef(pkOriginalColumns, pk);//get wich of the pk originals are referenced by the pk of this pk of the current table
                    if (fk == null){
                        continue;
                    }
                    query+= " ON " + pk.getCompletePrestoColumnName() + " = " + fk.getCompletePrestoColumnName() +" AND ";
                }
            }
        }
        query = query.substring(0, query.length() - " AND ".length());//last column is whithout a comma

        return query;
    }

    private Set<TableData> getAllDiferentLocalTablesInGlobalColumns(List<GlobalColumnData> gcs){
        Set<TableData> localTablesSelected = new HashSet<>();
        for (GlobalColumnData gc : gcs){
            for (ColumnData lc : gc.getLocalColumns()){
                localTablesSelected.add(lc.getTable());
            }
        }
        return localTablesSelected;
    }

    private ColumnData getForeignKeyRef(List<ColumnData> pkOriginal, ColumnData fk){
        for (ColumnData c : pkOriginal) {
            if (fk.getForeignKeySimplified().equals(c.getTable().getTableName() + "." + c.getName()));
            return c;
        }
        return null;
    }

    /*private String handleVerticalMapping(GlobalTableData t, List<GlobalColumnData> selectCols){
        //for each local table that matches with this global table
        String query = " SELECT ";
        String tableJoinString = "INNER JOIN ";

        Set<TableData> localTablesSelectCols = t.getLocalTablesFromColsVerticalMap(selectCols);
        //Set<TableData> localTables = t.getAllLocalTablesFromCols(t.getGlobalColumnDataList());
        Set<TableData> localTables = t.getAllLocalTablesFromCols(t.getGlobalColumnDataList());
        ColumnData primaryKeyCol = null;//primary key column of the original table (may be null if not queried any column or prim key)
        List<ColumnData> foreignKeyCols = new ArrayList<>();
        //all columns  from local tables (get primary key)
        for (TableData localTable : localTables){
            if (localTable.getColumnsList() == null)
                continue;
            Set<ColumnData> localCols = new HashSet<>(localTable.getColumnsList());
            for (ColumnData col: localCols){
                if (col.isPrimaryKey() && !col.hasForeignKey()){
                    primaryKeyCol = col;
                }
                else if (col.isPrimaryKey() && col.hasForeignKey() && localTableHasOneColumnInSelect(selectCols, localTable)) {//the foreign key table must have at least one column in the select clause
                    foreignKeyCols.add(col);
                }
            }
            //query+=localCols.get(localCols.size()-1).getCompletePrestoColumnName()+" ";//last column is whithout a comma
            //query+= "FROM "+localTable.getCompletePrestoTableName()+" ";
        }
        //get all local tables in the selected
        for (TableData localTable : localTablesSelectCols){
            Set<ColumnData> localCols = new HashSet<>(localTable.getColumnsList());
            for (ColumnData col: localCols){
                query+=col.getCompletePrestoColumnName() +", ";
            }
            //query+=localCols.get(localCols.size()-1).getCompletePrestoColumnName()+" ";//last column is whithout a comma
            //query+= "FROM "+localTable.getCompletePrestoTableName()+" ";
        }
        if (query.endsWith(", ")) {
            query = query.substring(0, query.length() - ", ".length());//last column is whithout a comma
        }
        if (localTablesSelectCols.size() == 1 && primaryKeyCol == null){
            //selected only columns from one table but not any primary key
            query+= " FROM "+localTablesSelectCols.iterator().next().getCompletePrestoTableName()+" ";//get the only local table present(no join needed)
        }
        else {
            if (primaryKeyCol != null) {
                query += " FROM " + primaryKeyCol.getTable().getCompletePrestoTableName() + " ";
                //add joins
                for (ColumnData col : foreignKeyCols) {
                    query += tableJoinString + " " + col.getTable().getCompletePrestoTableName() + " ON " + primaryKeyCol.getCompletePrestoColumnName() + " = " + col.getCompletePrestoColumnName();
                }
            }
            else{//TODO: test (more than 2 vertcial partioned tables)
                //if original table (with prim key) not used in query use the first foreign key to join
                String fkFullName = foreignKeyCols.get(0).getCompletePrestoColumnName();
                query += " FROM " + fkFullName + " ";
                for (int i = 1; i < foreignKeyCols.size(); i++) {
                    query += tableJoinString + " " + foreignKeyCols.get(i).getTable().getCompletePrestoTableName() + " ON " + fkFullName + " = " + foreignKeyCols.get(i).getCompletePrestoColumnName();
                }
            }
        }
        return query;
    }*/

    private boolean localTableCorrespondsToOneOfTheGlobalTables(List<GlobalColumnData> globalCols, ColumnData localCol){
        for (GlobalColumnData c : globalCols){
            if (c.getLocalColumns().contains(localCol)){
                return true;
            }
        }
        return false;
    }

    private boolean localTableHasOneColumnInSelect(List<GlobalColumnData> globalCols, TableData localTable){
        for (GlobalColumnData c : globalCols){
            for (ColumnData localCol : c.getLocalColumns()){
                if (localCol.getTable().equals(localTable))
                    return true;
            }
        }
        return false;
    }

    public void clearAllElements(){
        selectRows.clear();
        selectColumns.clear();
        measures.clear();
        orderBy.clear();
    }

    public Map<Integer, String> getMeasuresWithID() {
        Map<Integer, String> measuresWithOP = new HashMap<>();
        factsTable.getColumns().keySet();
        for (String m : measures){
            String measureName = getMeasureName(m);
            String measureOP = getMeasureOP(m);
            int id = getIDofMeasure(measureName);
            measuresWithOP.put(id, measureOP);
        }
        return measuresWithOP;
    }

    private int getIDofMeasure(String measureName){
        Set<GlobalColumnData> cols = factsTable.getColumns().keySet();
        for (GlobalColumnData c : cols){
            if (c.getName().equals(measureName))
                return c.getColumnID();
        }
        return -1;
    }

    public Map<GlobalTableData, List<GlobalColumnData>> getSelectRows() {
        return this.selectRows;
    }

    public void setSelectRows(Map<GlobalTableData, List<GlobalColumnData>> selectRows) {
        this.selectRows = selectRows;
    }

    public FactsTable getFactsTable() {
        return this.factsTable;
    }

    public void setFactsTable(FactsTable factsTable) {
        this.factsTable = factsTable;
    }

    public Map<GlobalTableData, List<GlobalColumnData>> getSelectColumns() {
        return this.selectColumns;
    }

    public void setSelectColumns(Map<GlobalTableData, List<GlobalColumnData>> selectColumns) {
        this.selectColumns = selectColumns;
    }

    public List<String> getMeasures() {
        return this.measures;
    }

    public void setMeasures(List<String> measures) {
        this.measures = measures;
    }

    public List<String> getOrderBy() {
        return this.orderBy;
    }

    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public List<List<String>> getPivotValues() {
        return pivotValues;
    }

    public void setPivotValues(List<List<String>> pivotValues) {
        this.pivotValues = pivotValues;
    }
}