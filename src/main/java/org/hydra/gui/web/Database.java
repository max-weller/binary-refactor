package org.hydra.gui.web;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.hydra.util.Log;
import org.hydra.util.Utils;

public class Database {
    private static String dbFileNameNew = "binary_refactor.sqlite";
    private static String dbFileNameOld = "simpledb.bin";

    private static Config config = new Config();
    private static IDatabase db;

    static {
        String path = config.getUploadPath();
        File dbNew = new File(path, dbFileNameNew);
        File dbOld = new File(path, dbFileNameOld);
        if (!dbNew.exists() && dbOld.exists()) {
            migrateDb(DB_Old_Style.load(dbOld), DB_Sqlite.load(dbNew));
        }

        db = DB_Sqlite.load(dbNew);
    }

    private static void migrateDb(IDatabase oldDb, IDatabase newDb) {
        Collection<Record> records = oldDb.listAll();
        for (Record record : records) {
            Log.debug("Migrating %s %s", record.getType(), record.getId());
            newDb.save(record.getType(), record.getId(), record.getObj());
        }
    }

    public static void save(String type, String id, Serializable obj) {
        db.save(type, id, obj);
    }

    public static void delete(String id) {
        db.delete(id);
    }

    public static Record get(String id) {
        return db.get(id);
    }

    public static List<Record> list(String type) {
        return db.list(type);
    }

    public static Config getConfig() {
        return config;
    }

    public static class Config {
        public String getUploadPath() {
            String home = System.getProperty("user.home");
            File workdir = new File(home, ".bf_workdir");
            if (workdir.exists() == false) {
                workdir.mkdirs();
            }
            try {
                return workdir.getCanonicalPath();
            } catch (IOException e) {
                return home;
            }
        }
    }

    public static class Record implements Serializable {
        private static final long serialVersionUID = -8089946506185400983L;
        private String id, type;
        private Serializable obj;

        public Record(String id, String type, Serializable obj) {
            this.id = id;
            this.type = type;
            this.obj = obj;
        }
        public Record(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
            this.id = rs.getString(1);
            this.type = rs.getString(2);
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(rs.getBinaryStream(3));
                this.obj = (Serializable) ois.readObject();
            } finally {
                Utils.close(ois);
            }
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public Serializable getObj() {
            return obj;
        }
    }

    public static class Util {
        public static String nextId() {
            return UUID.randomUUID().toString();
        }
    }

    public interface IDatabase {
        void save(String type, String id, Serializable obj);
        Record get(String id);
        List<Record> list(String type);
        Collection<Record> listAll();
        void delete(String id);
    }

    private static class DB_Sqlite implements IDatabase {
        private Connection dbcon;

        /**
         * id -> Record
         */
        private Map<String, Record> db = new ConcurrentHashMap<String, Record>();
        
        public DB_Sqlite(Connection connection) throws SQLException {
            this.dbcon = connection;
            initSchema();
        }

        public int getSchemaVersion() throws SQLException {
            Statement s = dbcon.createStatement();
            ResultSet rs = s.executeQuery("PRAGMA schema_version");
            if (!rs.next()) return -1;
            int version = rs.getInt(1);
            rs.close();
            return version;
        }

        private void initSchema() throws SQLException {
            Statement s = dbcon.createStatement();
            int version = getSchemaVersion();
            if (version < 100) {
                s.execute("CREATE TABLE IF NOT EXISTS simpledb (s_id TEXT UNIQUE, s_type TEXT, s_obj BLOB, dt DATETIME DEFAULT CURRENT_TIMESTAMP);");
                s.execute("PRAGMA schema_version=100;");
            }
        }

        private static byte[] getBytes(Object obj) throws java.io.IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            bos.close();
            byte[] data = bos.toByteArray();
            return data;
        }

        public void save(String type, String id, Serializable obj) {
            try {
                db.put(id, new Record(id, type, obj));
                PreparedStatement st = dbcon.prepareStatement("REPLACE INTO simpledb (s_id, s_type, s_obj) VALUES (?,?,?)");
                st.setString(1, id);
                st.setString(2, type);
                //st.setObject(3, obj);
                //   ...seems to be broken with SQLite ???
                //     https://stackoverflow.com/questions/9537728/objectinputstream-works-for-mysql-but-not-for-sqlite
                st.setBytes(3, getBytes(obj));
                st.executeUpdate();

            } catch(SQLException ex) {
                Log.error(ex.toString());
                throw new RuntimeException(ex.toString());
            } catch(IOException ex) {
                Log.error(ex.toString());
                throw new RuntimeException(ex.toString());
            }
        }

        private void store() {
            /* nothing to do */
        }

        @SuppressWarnings("unchecked")
		public static IDatabase load(File f) {
            try {
                // load the sqlite-JDBC driver using the current class loader
                Class.forName("org.sqlite.JDBC");

                // create a database connection
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + f.getPath());

                return new DB_Sqlite(connection);

            } catch(Exception ex) {
                Log.error(ex.toString());
                throw new RuntimeException(ex.toString());
            }
        }

        public Record get(String id) {
            if (db.containsKey(id)) return db.get(id);
            try {
                PreparedStatement st = dbcon.prepareStatement("SELECT s_id, s_type, s_obj FROM simpledb WHERE s_id = ?");
                st.setString(1, id);
                ResultSet rs = st.executeQuery();
                if (rs.next()) {
                    Record r = new Record(rs);
                    db.put(id, r);
                    return r;
                } else {
                    return null;
                }
            } catch(SQLException ex) {
                Log.error(ex.toString());
                return null;
            } catch(ClassNotFoundException ex) {
                Log.error(ex.toString());
                return null;
            } catch(IOException ex) {
                Log.error(ex.toString());
                return null;
            }
        }

        public List<Record> list(String type) {
            try {
                PreparedStatement st = dbcon.prepareStatement("SELECT s_id FROM simpledb WHERE s_type = ?");
                st.setString(1, type);
                ResultSet rs = st.executeQuery();
                List<Record> result = new ArrayList<Record>();
                while (rs.next()) {
                    result.add(get(rs.getString(1)));
                }
                return result;
            } catch(SQLException ex) {
                Log.error(ex.toString());
                throw new RuntimeException(ex.toString());
            }
        }

        public Collection<Record> listAll() {
            try {
                List<Record> result = new ArrayList<Record>();
                PreparedStatement st = dbcon.prepareStatement("SELECT s_id FROM simpledb");
                ResultSet rs = st.executeQuery();
                while(rs.next()) {
                    result.add(get(rs.getString(1)));
                }
                return result;
            } catch(SQLException ex) {
                Log.error(ex.toString());
                throw new RuntimeException(ex.toString());
            }
        }

        public void delete(String id) {
            try {
                List<Record> result = new ArrayList<Record>();
                PreparedStatement st = dbcon.prepareStatement("DELETE FROM simpledb WHERE s_id =  ?");
                st.setString(1, id);
                st.executeUpdate();
                db.remove(id);
            } catch(SQLException ex) {
                Log.error(ex.toString());
                throw new RuntimeException(ex.toString());
            }
        }

    }


    private static class DB_Old_Style implements IDatabase {

        private File dbFile;

        /**
         * id -> Record
         */
        private Map<String, Record> db = new ConcurrentHashMap<String, Record>();
        /**
         * type -> list of ids
         */
        private Map<String, List<String>> index = new ConcurrentHashMap<String, List<String>>();

        public DB_Old_Style(File f) {
            this.dbFile = f;
        }

        public void save(String type, String id, Serializable obj) {
            List<String> indexByType = this.index.get(type);
            if (indexByType == null) {
                indexByType = new ArrayList<String>();
                index.put(type, indexByType);
            }
            if (indexByType.contains(id) == false) {
                indexByType.add(id);
            }
            db.put(id, new Record(id, type, obj));
            store();
        }

        private void store() {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(new FileOutputStream(dbFile));

                oos.writeObject(this.db);
                oos.writeObject(this.index);
                oos.flush();
            } catch (Exception e) {
                Log.error(e.toString());
                Utils.close(oos);
            }
        }

        @SuppressWarnings("unchecked")
		public static IDatabase load(File f) {
            if (f.exists()) {
                ObjectInputStream ois = null;
                try {
                    ois = new ObjectInputStream(new FileInputStream(f));
                    DB_Old_Style db = new DB_Old_Style(f);
                    db.db = (Map<String, Record>) ois.readObject();
                    db.index = (Map<String, List<String>>) ois.readObject();
                    return db;
                } catch (Exception e) {
                    Log.error(e.toString());
                    Utils.close(ois);
                }
            }
            return new DB_Old_Style(f);
        }

        public Record get(String id) {
            return db.get(id);
        }

        public List<Record> list(String type) {
            List<String> indexByType = this.index.get(type);
            if (indexByType == null) {
                return null;
            } else {
                List<Record> result = new ArrayList<Record>(indexByType.size());
                for (String id : indexByType) {
                    result.add(db.get(id));
                }
                return result;
            }
        }

        public Collection<Record> listAll() {
            return db.values();
        }

        public void delete(String id) {
            Record rec = db.get(id);
            List<String> indexByType = this.index.get(rec.getType());
            if (indexByType != null) {
                indexByType.remove(id);
            }
            db.remove(id);
            store();
        }

    }
}
