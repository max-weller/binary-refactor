package org.hydra.gui.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hydra.renamer.ClassInfo;
import org.hydra.renamer.ClassMap;
import org.hydra.renamer.ClassMap.ClassWalker;
import org.hydra.renamer.MethodInfo;
import org.hydra.renamer.RenameConfig;
import org.hydra.util.Utils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import ru.andrew.jclazz.core.Clazz;
import ru.andrew.jclazz.core.ClazzException;
import ru.andrew.jclazz.decompiler.ClazzSourceView;
import ru.andrew.jclazz.decompiler.ClazzSourceViewFactory;

import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.output.ToStringDumper;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageCollectorImpl;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.attributes.AttributeInnerClasses;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/jarviewer")
public class ViewController {

    @RequestMapping("/view")
    public void view(Model model, @RequestParam("id") String jar,
            @RequestParam(value = "clz", required = false) String clazzName) {

        clazzName = clazzName != null ? clazzName.replace('.', '/') : null;

        model.addAttribute("clzName", clazzName);
        model.addAttribute("id", jar);

        if (Database.get(jar) == null) {
            return;
        }

        FileItem path = (FileItem) Database.get(jar).getObj();
        try {
            ClassMap classMap = ClassMap.build(new File(path.getFullName()));

            classMap.rebuildConfig(new RenameConfig(), null);

            model.addAttribute("classMap", classMap);
            model.addAttribute("origName", path.getOrigName());

            // to find which package should open
            if (clazzName != null) {
                model.addAttribute("openPkg", classMap.getShortPackage(clazzName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @RequestMapping(value="/filetree", produces="application/json")
    //@RequestMapping(value="/filetree", produces="text/xml")
    @ResponseBody
    public String filetree(Model model, @RequestParam("id") String jar,
            @RequestParam(value = "clz", required = false) String clazzName) {
        
        
        model.addAttribute("id", jar);

        if (Database.get(jar) == null) {
            return null;
        }

        FileItem path = (FileItem) Database.get(jar).getObj();
        try {
            File jarFileInfo = new File(path.getFullName());
            ClassMap classMap = ClassMap.build(jarFileInfo);
            HashMap<String,Object> output = new HashMap<String,Object>();
            output.put("jarId", jar);
            output.put("jarName", jarFileInfo.getName());
            List<HashMap<String,String>> classes = new ArrayList<HashMap<String,String>>();
            output.put("classes", classes);
            for(ClassInfo clazz : classMap.getClasses()) {
                HashMap<String,String> obj = new HashMap<String,String>();
                classes.add(obj);
                //obj.put("viewUrl", "/jarviewer/view.htm?id="+jar+"&clz="+clazz.getClassName());
                String className = clazz.getClassName();
                int pos = className.lastIndexOf('/');
                obj.put("package", className.substring(0, pos));
                obj.put("shortName", className.substring(pos + 1));
            }
/*
            HashMap<String,List<String>> xref = new HashMap<String,List<String>>();
            output.put("xref", xref);
            for(Map.Entry<String, List<MethodInfo>> x : classMap.getXref().entrySet()) {
                ArrayList<String> list = new ArrayList<String>(x.getValue().size());
                for(MethodInfo i : x.getValue()) {
                    list.add(i.getEnclosedClass().getClassName()+"#"+i.getName()+"__"+i.getDesc());
                }
                xref.put(x.getKey(), list);
            }
*/
            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(output);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping("/graph")
    public void graph(Model model, @RequestParam("id") String jar,
            @RequestParam(value = "clz", required = false) String clz,
            @RequestParam(value = "type", required = false) final String type) {
        model.addAttribute("id", jar);
        model.addAttribute("clz", clz);
        model.addAttribute("type", type);
    }

    @RequestMapping("/graphdata")
    public View graphdata(@RequestParam("id") final String jar,
            @RequestParam(value = "clz", required = false) final String clz,
            @RequestParam(value = "type", required = false) final String type) {
        FileItem path = (FileItem) Database.get(jar).getObj();
        try {
            final ClassMap classMap = ClassMap.build(new File(path.getFullName()));
            classMap.rebuildConfig(new RenameConfig(), null);
            return new View() {

                @Override
                public String getContentType() {
                    return "application/json";
                    // return "text/plain";
                }

                @Override
                public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
                        throws Exception {
                    BufferedWriter writer = new BufferedWriter(response.getWriter());
                    Map<String, List<ClassInfo>> tree = classMap.getTree();
                    int count = 0;
                    Set<String> allObj = new HashSet<String>();
                    writer.write("{\"src\":\"");
                    ClassInfo clzInfo = classMap.getClassInfo(clz);
                    if (clzInfo != null) {
                        // partial dep-graph
                        if ("mydeps".equals(type)) {
                            // deps of clz
                            for (String dep : clzInfo.getDependencies()) {
                                if (classMap.contains(dep)) {
                                    writeDep(writer, clz, dep);
                                }
                            }
                        } else if ("depsonme".equals(type)) {
                            // who depends on me ?
                            for (Map.Entry<String, List<ClassInfo>> e : tree.entrySet()) {
                                for (ClassInfo info : e.getValue()) {
                                    if (info.getDependencies().contains(clz)) {
                                        writeDep(writer, info.getClassName(), clz);
                                    }
                                }
                            }
                        }
                        writer.write(clz + " {color:red,link:'/'}");
                    } else {
                        // all data
                        for (Map.Entry<String, List<ClassInfo>> e : tree.entrySet()) {
                            writer.write(";" + e.getKey() + "\\n");
                            for (ClassInfo clazz : e.getValue()) {
                                for (String dep : clazz.getDependencies()) {
                                    if (classMap.contains(dep)) {
                                        allObj.add(clazz.getClassName());
                                        allObj.add(dep);
                                        writeDep(writer, clazz.getClassShortName(), Utils.getShortName(dep));
                                        count++;
                                    }
                                }
                            }
                            writer.write(";// end of package " + e.getKey() + "\\n");
                        }
                    }
                    writer.write("; totally " + count + " edges.\\n");
                    // TODO link doesn't work,why?
                    // for (String clz : allObj) {
                    // writer.write(Utils.getShortName(clz) +
                    // " {link:'view.htm?id=" + jar + "&clz=" + clz + "'}\\n");
                    // }
                    writer.write("\"}\n");
                    writer.flush();
                    Utils.close(writer);
                }

                private void writeDep(Writer writer, final String src, String dest) throws IOException {
                    writer.write(src);
                    writer.write(" -> ");
                    writer.write(dest);
                    writer.write("\\n");
                }

            };
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @RequestMapping("/source")
    public void source(Model model, @RequestParam("id") String jarid, @RequestParam("clz") String clazzName,
                       @RequestParam(value = "type", required = false, defaultValue = "asmdump") String type) {
        model.addAttribute("clzName", clazzName);
        model.addAttribute("id", jarid);

        if (Database.get(jarid) == null) {
            return;
        }

        FileItem path = (FileItem) Database.get(jarid).getObj();
        model.addAttribute("jarFile", path);
        try {
            String code = "";
            JarFile jarFile = new JarFile(path.getFullName());

            JarEntry entry = jarFile.getJarEntry(clazzName + ".class");
            InputStream inputStream = jarFile.getInputStream(entry);

            if (type.equals("asmdump")) {
                code = asmDump(inputStream);
            } else if (type.equals("decomp")) {
                code = jclazzDecomp(clazzName + ".class", inputStream);
            }
            model.addAttribute("code", code);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @RequestMapping(value="/xref", produces="application/json")
    @ResponseBody
    public String xref(Model model, @RequestParam("id") String jarid, @RequestParam("clz") String clazzName,
                       @RequestParam("member") String memberName,@RequestParam("sig") String signature) {

        if (Database.get(jarid) == null) {
            return "{\"error\":true}";
        }

        FileItem path = (FileItem) Database.get(jarid).getObj();

        try {
            ClassMap map = ClassMap.build(new File(path.getFullName()));
            List<MethodInfo> xref = map.getXref(clazzName, memberName, signature);
            if (xref == null) return "[]";
            ArrayList<String> list = new ArrayList<String>(xref.size());
            for(MethodInfo i : xref) {
                list.add(i.getEnclosedClass().getClassName()+"#"+i.getName()+"__"+i.getDesc());
            }

            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(list);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return  "{\"error\":true}";
        }

    }

    @RequestMapping("/cfrsource")
    public void cfrSource(Model model, @RequestParam("id") String jarid, @RequestParam("clz") String clazzName) {
        model.addAttribute("clzName", clazzName);
        model.addAttribute("id", jarid);

        if (Database.get(jarid) == null) {
            return;
        }

        FileItem path = (FileItem) Database.get(jarid).getObj();
        model.addAttribute("jarFile", path);
        String code = "";
        code = cfrDecomp(path.getFullName(),
            "/jarviewer/cfrsource.htm?id="+jarid+"&clz=",
            clazzName + ".class");
        model.addAttribute("code", code);
        

    }

    private String asmDump(InputStream inputStream) throws IOException {
        ClassReader reader = new ClassReader(inputStream);

        StringWriter writer = new StringWriter();
        reader.accept(new TraceClassVisitor(new PrintWriter(writer)), 0);
        String code = writer.toString().trim();
        return code;
    }

    private String jclazzDecomp(String clazzName, InputStream inputStream) {
        try {
            Clazz clazz = new Clazz(clazzName,inputStream);
            ClazzSourceView csv = ClazzSourceViewFactory.getClazzSourceView(clazz);
            return csv.getSource();
        } catch (ClazzException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String cfrDecomp(String jarfilename, String urlPrefix, String clazzName) {
        try {
            Map<String, String> optionsMap = new HashMap<String, String>();
            Options options = new OptionsImpl(optionsMap);
            ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);

            classFileSource.clearConfiguration();
            DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
            //DumperFactory dumperFactory = new DumperFactoryImpl(options);

            //List<JavaTypeInstance> types = 
            dcCommonState.explicitlyLoadJar(jarfilename);
            //classFileSource.addJar(jarfilename);

            //String classfilepath = ClassNameUtils.convertToPath(clazzName) + ".class";
            ClassFile c = dcCommonState.getClassFile(clazzName);
            c.loadInnerClasses(dcCommonState);
            c.analyseTop(dcCommonState);

            TypeUsageCollector collectingDumper = new TypeUsageCollectorImpl(c);
            c.collectTypeUsages(collectingDumper);
            //d = dumperFactory.getNewTopLevelDumper(c.getClassType(), summaryDumper, collectingDumper.getTypeUsageInformation(), illegalIdentifierDump);
                // return new StdIODumper(typeUsageInformation, this.options, illegalIdentifierDump);

            //c.dump(d);
            //return ToStringDumper.toString(c);
            AttributeInnerClasses attributeInnerClasses = (AttributeInnerClasses)c.getAttributeByName("InnerClasses");
            if(attributeInnerClasses == null){
                System.out.println("has no inner classes");
            }else {
                 List<InnerClassAttributeInfo> list=attributeInnerClasses.getInnerClassAttributeInfoList();
                 for (InnerClassAttributeInfo innerClassAttributeInfo : list) {
                    JavaTypeInstance innerType = innerClassAttributeInfo.getInnerClassInfo();
                    System.out.println("inner class: "+innerType.getRawName());
                 }
            }
        

            CfrToHtmlDumper dumper = new CfrToHtmlDumper(collectingDumper.getTypeUsageInformation());
            dumper.urlPrefix = urlPrefix;
            return dumper.dump(c).toString();

            //return CfrToHtmlDumper.toString(c);

        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    @RequestMapping(value = "download")
    public View download(@RequestParam("id") String jarid) {
        FileItem path = (FileItem) Database.get(jarid).getObj();
        StreamView view = null;
        try {
            File file = new File(path.getFullName());
            view = new StreamView("application/x-jar", new FileInputStream(file));
            view.setBufferSize(4 * 1024);
            view.setContentDisposition("attachment; filename=\"" + path.getOrigName() + "\"");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return view;
    }

    @RequestMapping(value = "search")
    public String search(Model model, @RequestParam("id") String jarid, @RequestParam("q") final String query) {
        model.addAttribute("id", jarid);
        model.addAttribute("query", query);

        if (Database.get(jarid) == null) {
            return null;
        }

        FileItem path = (FileItem) Database.get(jarid).getObj();
        model.addAttribute("jarFile", path);
        try {
            ClassMap classMap = ClassMap.build(new File(path.getFullName()));

            classMap.rebuildConfig(new RenameConfig(), null);

            model.addAttribute("classMap", classMap);

            final Set<String> matches = new TreeSet<String>();
            ClassWalker walker = new ClassWalker() {

                @Override
                public void walk(ClassInfo classInfo) {
                    if (classInfo.getClassShortName().equals(query)) {
                        matches.add(classInfo.getClassName());
                    }
                }
            };

            classMap.walk(walker);

            if (matches.size() == 1) {
                model.addAttribute("clz", matches.iterator().next());
                return "redirect:/jarviewer/view.htm";
            }
            model.addAttribute("matches", matches);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "/jarviewer/search";
    }

}
