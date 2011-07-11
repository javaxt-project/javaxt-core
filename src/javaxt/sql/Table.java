package javaxt.sql;
import java.sql.*;

public class Table implements Comparable {
    
    private DatabaseMetaData dbmd = null;
    
    private String Name;
    private String Description;
    private String Schema;
    private String Catalog;
    
    
    
    protected Table(ResultSet rs, DatabaseMetaData dbmd){
        try{            
            Name = rs.getString("TABLE_NAME");
            Description = rs.getString("REMARKS");
            Catalog = rs.getString("TABLE_CAT");
            Schema = rs.getString("TABLE_SCHEM");
            this.dbmd = dbmd;
        }
        catch(Exception e){
            
        }
    }
    
    
    
    private Table(){
    }
    
    public String getName(){return Name;}
    public String getDescription(){return Description;}
    public String getSchema(){return Schema;}
    public String getCatalog(){return Catalog;}
    
    
    
    
    
  //**************************************************************************
  //** getColumns
  //**************************************************************************
  /** Used to retrieve an array of all the columns found in this table. 
   */
    
    public Column[] getColumns(){
        try{
            
            java.util.Vector columns = new java.util.Vector();
            Key[] Keys = getPrimaryKeys();
            Key[] FKeys = getForeignKeys();


            ResultSet rs = dbmd.getColumns(this.Catalog,this.Schema,this.Name,null);
            while (rs.next()) {

              //Create Column
                Column column = new Column(rs, this);

                
              //Set Primary Key
                if (Keys!=null){
                    for (int i=0; i<Keys.length; i++){
                         if (column.getName().equals(Keys[i].Name)){
                             column.setIsPrimaryKey(true);
                         }
                    }
                }

              //Set Foreign Key
                if (FKeys!=null){
                    for (int i=0; i<FKeys.length; i++){
                         if (column.getName().equals(FKeys[i].Name)){
                             column.setForeignKey(FKeys[i]);
                         }
                    }
                }
                
              //Add Column to the Vector
                columns.add(column);
                
            }
            
            rs.close();
            
          //Convert the Vector to an Array
            Column[] array = new Column[columns.size()];
            for (int i=0; i<array.length; i++){
                array[i] = (Column) columns.get(i);
            }
            
          //Return the array of columns
            return array;
            
        }
        catch(Exception e){
            return null;
        }
    }


    
    
  //**************************************************************************
  //** getPrimaryKeys
  //**************************************************************************
  /** Used to retrieve the primary keys in this table. Usually there is only
   *  one primary key per table, but some vendors do support multiple keys per 
   *  table.
   */
    
    public Key[] getPrimaryKeys(){
        try{
            
            java.util.Vector keys = new java.util.Vector();
            
            ResultSet rs = dbmd.getPrimaryKeys(this.Catalog,this.Schema,this.Name);
            while (rs.next()) {

                Key key = new Key();
                key.Name = rs.getString("COLUMN_NAME");

                keys.add(key);

            }            
            rs.close();
            
            Key[] array = new Key[keys.size()];
            for (int i=0; i<array.length; i++){
                array[i] = (Key) keys.get(i);
            }
            return array;
            
        }
        catch(Exception e){
            return null;
        }
    }

    
  //**************************************************************************
  //** getForeignKeys
  //**************************************************************************
  /** Used to retrieve the foriegn keys found in this table. 
   */
    
    public Key[] getForeignKeys(){
        try{
            
            java.util.Vector keys = new java.util.Vector();
            ResultSet rs = dbmd.getImportedKeys(this.Catalog,this.Schema,this.Name);
            while (rs.next()) {

                Key Key = new Key();
                Key.Name = rs.getString("FKCOLUMN_NAME");
                Key.Table = new Table();
                Key.Table.Name = rs.getString("PKTABLE_NAME");
                Key.Table.Catalog = rs.getString("PKTABLE_CAT");
                Key.Table.Schema = rs.getString("PKTABLE_SCHEM");
                Key.Column = rs.getString("PKCOLUMN_NAME");

                keys.add(Key);

            }
            rs.close();
            
            
            Key[] array = new Key[keys.size()];
            for (int i=0; i<array.length; i++){
                array[i] = (Key) keys.get(i);
            }
            return array;
            
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /**  Returns the table name. */
    
    public String toString(){
        return this.getName();
    }

    public int hashCode(){
        return this.toString().hashCode();
    }

    //@Override
    public int compareTo(Object obj){
        if (obj==null) return -1;
        else return -obj.toString().compareTo(toString());
    }
}
