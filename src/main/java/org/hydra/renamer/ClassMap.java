package org.hydra.renamer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.hydra.gui.web.XrefResultItem;
import org.hydra.renamer.RenameConfig.ClassRenameInfo;
import org.hydra.renamer.asm.CollectClassInfoVisitor;
import org.hydra.util.Log;
import org.hydra.util.Utils;
import org.objectweb.asm.ClassReader;

public class ClassMap {
    private Map<String, ClassInfo> map = new TreeMap<String, ClassInfo>();
    private Map<String, List<MethodInfo>> methodCallXref = new HashMap<String, List<MethodInfo>>();

    private long fileTimestamp;

    public List<MethodInfo> getXref(String classname, String name, String sig) {
        return methodCallXref.get(classname+" "+name+" "+sig);
    }
    public  Map<String, List<MethodInfo>>  getXref() {
        return methodCallXref;
    }
    public void addXref(String to, MethodInfo location) {
        if (!methodCallXref.containsKey(to)) {
            methodCallXref.put(to, new ArrayList<MethodInfo>());
        }
        methodCallXref.get(to).add(location);
    }

    public ClassInfo getClassInfo(String name) {
        if (name == null) {
            return null;
        }
        return this.map.get(name.replace('.', '/'));
    }

    public void walk(ClassWalker walker) {
        for (Map.Entry<String, ClassInfo> entry : map.entrySet()) {
            walker.walk(entry.getValue());
        }
    }

    public boolean contains(String name) {
        return this.getClassInfo(name) != null;
    }

    public Collection<ClassInfo> getClasses() {
        return map.values();
    }

    public Map<String, List<ClassInfo>> getTree() {
        Map<String, List<ClassInfo>> result = new TreeMap<String, List<ClassInfo>>();

        for (Map.Entry<String, ClassInfo> entry : map.entrySet()) {
            String pkg = getShortPackage(entry.getKey());
            List<ClassInfo> list = result.get(pkg);
            if (list == null) {
                list = new ArrayList<ClassInfo>();
                result.put(pkg, list);
            }
            list.add(entry.getValue());
        }
        return result;
    }

    public String getShortPackage(String key) {
        int index = key.lastIndexOf("/");
        String fullName = key.substring(0, index);
        StringBuilder shortName = new StringBuilder();

        String[] arr = fullName.split("/");
        // 除了最后一层包名外，前面的都只取第一个字符
        for (int i = 0; i < arr.length - 1; i++) {
            //shortName.append(arr[i].charAt(0)).append("/");
            shortName.append(arr[i]).append("/"); // dont shorten the names....
        }

        shortName.append(arr[arr.length - 1]);
        return shortName.toString();
    }

    public void addClassInfo(ClassInfo info) {
        this.map.put(info.getClassName(), info);
    }

    public void rebuildConfig(RenameConfig config, String addPkgs) {
        // build the hierarchy and rebuild the Config
        for (Map.Entry<String, ClassInfo> entry : map.entrySet()) {
            ClassInfo info = entry.getValue();
            if (info.getSuperClass() != null) {
                info.getSuperClass().addChild(info.getClassName());
            }
            for (ClassInfo intf : info.getInterfaces()) {
                intf.addChild(info.getClassName());
            }
        }
        List<String> addPkgsList = Arrays.asList(Utils.tokens(addPkgs));
        if (addPkgsList != null) {
            for (String name : map.keySet()) {
                if (!config.getConfig().containsKey(name)) {
                    config.getConfig().put(name, new ClassRenameInfo());
                }
            }
        }
        // 重建 config ，主要是根据类／接口的层次结构修改
        for (Map.Entry<String, ClassRenameInfo> entry : config.getConfig().entrySet()) {
            String clazz = entry.getKey();
            ClassInfo classInfo = map.get(clazz);
            if (classInfo == null) {
                Log.debug("classInfo not found for %s", clazz);
            } else {
                entry.getValue().setSuperClassName(
                        classInfo.getSuperClass() == null ? null : classInfo.getSuperClass().getClassName());
                if (classInfo.getInterfaces().size() > 0) {
                    for (ClassInfo i : classInfo.getInterfaces()) {
                        entry.getValue().addInterface(i.getClassName());
                    }
                }
            }
        }
    }

    public String toString() {
        return String.format("ClassMap {%s}", map);
    }

    public static HashMap<String, ClassMap> cache = new HashMap<String, ClassMap>();

    public static ClassMap build(File fileInfo) throws IOException {
        if (cache.containsKey(fileInfo.getCanonicalPath())) {
            ClassMap m = cache.get(fileInfo.getCanonicalPath());
            if (fileInfo.lastModified() <= m.fileTimestamp)
                return cache.get(fileInfo.getCanonicalPath());
        }
        JarFile jar = new JarFile(fileInfo);
        ClassMap map = new ClassMap();
        map.fileTimestamp = fileInfo.lastModified();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                InputStream inputStream = null;
                try {
                    inputStream = jar.getInputStream(jar.getEntry(name));
                    // .class 文件就检查并改写
                    parseClass(inputStream, map);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(inputStream);
                }
            }
        }
        cache.put(fileInfo.getCanonicalPath(), map);
        return map;
    }

    private static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                // null
            }
        }
    }

    private static void parseClass(InputStream inputStream, ClassMap map) {
        CollectClassInfoVisitor cv = new CollectClassInfoVisitor(map);
        ClassReader reader;
        try {
            reader = new ClassReader(inputStream);
            reader.accept(cv, 8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static interface ClassWalker {
        public void walk(ClassInfo classInfo);
    }

}
