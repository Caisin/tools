package com.caisin.tools.convert;

import com.caisin.tools.utils.FileUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import sun.rmi.rmic.iiop.DirectoryLoader;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Lin
 * @date 2020-05-09
 */
public class ConvertError {
    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, TemplateException {
        File home = new File("/Users/Lin/WorkSource/Git/lucl/acctmanm-old/acctmanm-hnan/acctmanm-cvt/drecv");
        List<File> files = new LinkedList<>();
        files.add(new File("/Users/Lin/WorkSource/Git/lucl/IAcctException.java"));
        filter(files, home);

        File out = new File("/Users/Lin/WorkSource/Git/lucl/output");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager sjfm = compiler.getStandardFileManager(null, null, null);
        sjfm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(out));
        Iterable units = sjfm.getJavaFileObjects(files.toArray(new File[0]));

        JavaCompiler.CompilationTask ct = compiler.getTask(null, sjfm, null, null, null, units);
        ct.call();
        sjfm.close();

        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setDirectoryForTemplateLoading(
                new File(ConvertError.class.getClassLoader().getResource("template").getFile()));
        Template template = configuration.getTemplate("Error.ftl");

        Map<String, String> nameMapping = new HashMap<>();
        ClassLoader loader = new DirectoryLoader(out);
        for (File file : files) {
            if ("IAcctException.java".equals(file.getName())) {
                continue;
            }
            String path = file.getAbsolutePath();
            String outPath = path.replace("Exception.java", "Error.java");

            int idx = path.indexOf("/com/");
            path = path.substring(idx + 1, path.length() - 5);
            String className = path.replace('/', '.');
            String packageName = className.substring(0, className.lastIndexOf('.'));

            Class<?> clazz = loader.loadClass(className);
            if (clazz.isEnum()) {
                Object[] objs = clazz.getEnumConstants();
                Method codeMethod = clazz.getDeclaredMethod("getCode");
                Method messageMethod = clazz.getDeclaredMethod("getMessage");

                String outClassName = clazz.getSimpleName().replace("Exception", "Error");
                nameMapping.put(clazz.getSimpleName(), outClassName);
                List<Map<String, Object>> list = new LinkedList<>();
                for (Object o : objs) {
                    Map<String, Object> map = new HashMap<>();
                    Object name = codeMethod.invoke(o);
                    if (name == null) {
                        name = o.toString();
                    }
                    map.put("name", name);
                    map.put("message", messageMethod.invoke(o));
                    list.add(map);
                }
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outPath))))) {
                    // step5 生成数据
                    File docFile = new File(outPath);
                    Map<String, Object> dataMap = new HashMap<String, Object>();
                    dataMap.put("packageName", packageName);
                    dataMap.put("className", outClassName);
                    dataMap.put("list", list);
                    // step6 输出文件
                    template.process(dataMap, writer);
                }
            }
        }
        nameMapping.put("AcctCompException", "AcctCompError");
        FileUtils.deepSearchDo(home, (file) -> {
            String fileName = file.getName();
            if (fileName.endsWith(".java") && !fileName.endsWith("Exception.java") && !fileName.endsWith("Error.java")) {
                List<String> lines = new LinkedList<>();
                boolean change = false;

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        for (Map.Entry entry : nameMapping.entrySet()) {
                            String key = entry.getKey().toString();
                            String value = entry.getValue().toString();
                            if (line.indexOf(key) >= 0) {
                                line = line.replaceAll(key, value);
                                change = true;
                            }
                        }
                        lines.add(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (change) {
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                        lines.forEach((line)-> {
                            try {
                                bw.write(line);
                                bw.newLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        });

    }

    static void filter(List<File> files, File home) {
        for (File file : home.listFiles()) {
            if (file.getName().endsWith("Exception.java")) {
                files.add(file);
            } else if (file.isDirectory()) {
                filter(files, file);
            }
        }

    }
}
