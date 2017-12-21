package util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by Administrator on 2017/8/4.
 */
public class DbQueryUtil {
    private static Logger logger = Logger.getLogger(DbQueryUtil.class);
    private static DbQueryUtil instance = null;

    public static DbQueryUtil getInstance(){
        if (instance == null) {
            synchronized (DbQueryUtil.class) {
                if (instance == null) {
                    instance = new DbQueryUtil();
                }
                return instance;
            }
        }
        return instance;
    }
    //原insert方法，为了兼容5.0版本mysql先注释掉
//    public int insertRow(Connection connection,String iSql,boolean iIsReturnGeneratedKeys){
//        logger.debug("execute sql : "+iSql);
//        try(Statement stmt = connection.createStatement();
//            ResultSet rs = stmt.getGeneratedKeys()){
//            if(iIsReturnGeneratedKeys == false){
//                stmt.execute(iSql);
//                return 0;
//            }else{
//                stmt.execute(iSql,Statement.RETURN_GENERATED_KEYS);
//                if(rs.next()){
//                    int resInt = rs.getInt(1);
//                    return resInt;
//                }
//                return -1;
//            }
//        }catch (SQLException e){
//            logger.error("",e);
//            return -1;
//        }
//    }

    //为了兼容5.0版本mysql先用这个
    public int insertRow(Connection connection,String iSql,boolean iIsReturnGeneratedKeys){
        logger.debug("execute sql : "+iSql);
        try(Statement stmt = connection.createStatement()){
                stmt.execute(iSql);
                return 0;
        }catch (SQLException e){
            logger.error("",e);
            return -1;
        }
    }

    public boolean updateRow(Connection connection,String iSql){
        boolean returnFlag = false;
        try(Statement stmt = connection.createStatement()){
            logger.debug("execute sql : "+iSql);
            returnFlag = stmt.execute(iSql);
        }catch (SQLException e){
            logger.error(" 更新出错 ： ",e);
        }
        return returnFlag;
    }

    public boolean deleteRow(Connection connection,String iSql){
        boolean returnFlag = false;
        try(Statement stmt = connection.createStatement()){
            logger.debug("execute sql : "+iSql);
            returnFlag = stmt.execute(iSql);
        }catch (SQLException e){
            logger.error("",e);
        }
        return returnFlag;
    }



    //获取列名
    private ArrayList<String> getFieldList(ResultSetMetaData iRsmd)throws SQLException {
        ArrayList<String> fieldList = new ArrayList<String>();
        for(int i=1;i<=iRsmd.getColumnCount();i++){
            fieldList.add(iRsmd.getColumnLabel(i));
        }
        return fieldList;
    }
    //---
    private ArrayList<String> getFieldTypeList(ResultSetMetaData iRsmd)throws SQLException{
        ArrayList<String> fieldList = new ArrayList<String>();
        for(int i=1;i<=iRsmd.getColumnCount();i++){
            fieldList.add(iRsmd.getColumnTypeName(i));
        }
        return fieldList;
    }


    public JSONObject selectJSONObject(Connection connection, String iSql){
        logger.debug(iSql);
        JSONObject rowObj = new JSONObject();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try(Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(iSql)){
            ResultSetMetaData rsmd = rs.getMetaData();
            ArrayList<String> fieldList = getFieldList(rsmd);
            ArrayList<String> fieldTypeList = getFieldTypeList(rsmd);
            if(rs.next()){
                for(int i = 0; i<fieldList.size() ;i++){
                    if(fieldTypeList.get(i).equalsIgnoreCase("dataTime")){
                        if(rs.getDate(i+1)!=null){
                            rowObj.put(fieldList.get(i),sdf.format(rs.getDate(i+1)));
                        }else {
                            rowObj.put(fieldList.get(i),"");
                        }
                    }else {
                        if(rs.getObject(i+1)!=null){
                            rowObj.put(fieldList.get(i),rs.getObject(i+1));
                        }else {
                            rowObj.put(fieldList.get(i),"");
                        }
                    }
                }
            }
        }catch (SQLException e){
            logger.error("",e);
        }
        return rowObj;
    }


    public JSONArray selectJSONArrayMap(Connection connection, String iSql){
        logger.info("execute sql: "+iSql);
        JSONArray jsonArray = new JSONArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try(Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(iSql)){
            ResultSetMetaData rsmd = rs.getMetaData();
            ArrayList<String> fieldList = getFieldList(rsmd);
            ArrayList<String> fieldTypeList = getFieldTypeList(rsmd);
            while (rs.next()){
                JSONObject rowObject = new JSONObject();
                for (int i =0;i<fieldList.size();i++){
                    if(fieldTypeList.get(i).equalsIgnoreCase("dataTime")){
                        if(rs.getDate(i+1)!=null){
                            rowObject.put(fieldList.get(i),sdf.format(rs.getDate(i+1)));
                        }else {
                            rowObject.put(fieldList.get(i),"");
                        }
                    }else {
                        if(rs.getObject(i+1)!=null){
                            rowObject.put(fieldList.get(i),rs.getObject(i+1));
                        }else {
                            rowObject.put(fieldList.get(i),"");
                        }
                    }
                }
                jsonArray.add(rowObject);
            }
        }catch (SQLException e){
            logger.error("",e);
        }
        return jsonArray;
    }


    public Object selectValue(Connection connection,String iSql){
        try(Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(iSql)){
            if(rs.next()){
                Object resObj = rs.getObject(1);
                rs.close();
                return resObj;
            }
        }catch (SQLException e){
            logger.error("",e);
        }
        return null;
    }
}
