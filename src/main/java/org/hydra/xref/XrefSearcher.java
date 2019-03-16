package org.hydra.xref;

import org.hydra.renamer.ClassMap;
import org.hydra.renamer.RenameConfig;
import org.hydra.renamer.asm.ClassForNameFixVisitor;
import org.hydra.renamer.asm.CollectClassInfoVisitor;
import org.hydra.renamer.asm.Remapper;
import org.hydra.util.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class XrefSearcher {


   /* public XrefSearcher(String jarFile) {
        JarFile jar = new JarFile(jarFile);

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                InputStream inputStream = null;
                try {
                    inputStream = jar.getInputStream(jar.getEntry(name));
                    // .class 文件就检查并改写
                    CollectClassInfoVisitor cv = new CollectClassInfoVisitor(map);
                    ClassReader reader;
                    try {
                        reader = new ClassReader(inputStream);
                        reader.accept(cv, 8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(inputStream);
                }
            }
        }
        return map;

    }
    public static void rename(RenameConfig config, String jarFile, String destFile) {
        try {
            JarFile jar = new JarFile(jarFile);
            // 解析整个jar文件，得到ClassMap
            ClassMap classMap = ClassMap.build(jar);
            Log.debug("Parsed config:\n%s", config.getConfig());
            classMap.rebuildConfig(config, null);
            // Log.debug("Rebuild config:\n%s", config.getConfig());
            Remapper remapper = new Remapper(config);

            Enumeration<JarEntry> entries = jar.entries();

            JarOutputStream zos = new JarOutputStream(new FileOutputStream(destFile));
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                // System.out.println("deal with " + entry.getName());
                if (entry.isDirectory()) {
                    // 目录就直接写入
                    zos.putNextEntry(entry);
                    zos.closeEntry();
                } else {
                    String name = entry.getName();
                    InputStream inputStream = jar.getInputStream(jar.getEntry(name));
                    if (name.endsWith(".class")) {
                        // .class 文件就检查并改写
                        byte[] bytes = renameClazz(remapper, inputStream);
                        int idex = name.lastIndexOf('.');

                        JarEntry en = new JarEntry(remapper.map(name.substring(0, idex)) + ".class");
                        zos.putNextEntry(en);
                        zos.write(bytes);
                        zos.closeEntry();
                    } else {
                        // 其他资源直接写入
                        zos.putNextEntry(entry);
                        byte[] buff = new byte[10240];
                        int cnt = 0;
                        while ((cnt = inputStream.read(buff)) > 0) {
                            zos.write(buff, 0, cnt);
                        }
                        zos.closeEntry();
                    }
                    inputStream.close();
                }
            }
            zos.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private static byte[] renameClazz(Remapper remapper, InputStream clazz) {
        try {
            ClassWriter writer = new ClassWriter(1);
            ClassReader reader = new ClassReader(clazz);
            reader.accept(new ClassForNameFixVisitor(new RemappingClassAdapter(writer, remapper), remapper), 8);
            return writer.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }*/
}
