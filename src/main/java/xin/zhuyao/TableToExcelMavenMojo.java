package xin.zhuyao;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import xin.zhuyao.annotation.FieldName;
import xin.zhuyao.annotation.TableName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * @author zhuyao
 */
@Mojo(name = "tableToExcelMaven")
public class TableToExcelMavenMojo extends AbstractMojo {


    @Parameter(property = "scanPackage",defaultValue = "")
    private String scanPackage;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true)
    private String[] compileClasspathElements;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true)
    private String artifactId;

    private URLClassLoader loader;


    public void execute() {

        String file = compileClasspathElements[0].replace("classes","");

        String classPath = compileClasspathElements[0];
        String libPath = compileClasspathElements[0].replace("classes",artifactId+"/WEB-INF/lib");
        String basePackage = compileClasspathElements[0]+"/"+scanPackage.replaceAll("\\.","/");

        try {
            String libDir = (new URL("file",null,new File(libPath).getCanonicalPath()+File.separator)).toString();
            String baseDir = (new URL("file",null,new File(classPath).getCanonicalPath()+File.separator)).toString();

            File libDirFile = new File(libDir.replaceAll("file",""));
            URLStreamHandler us = null;
            List<URL> libs = new ArrayList<URL>();
            if (null!=libDirFile.listFiles()){
                for (File jar : libDirFile.listFiles()) {
                    libs.add(new URL(null,libDir+jar.getName(),us));
                }
            }
            libs.add(new URL(null,baseDir,us));
            loader = new URLClassLoader(libs.toArray(new URL[libs.size()]),Thread.currentThread().getContextClassLoader());
            File dir = new File(basePackage);
            List<Class<?>> classes = new ArrayList<Class<?>>();
            scanner(classes,dir,classPath);

            List<List<Object>> objectList = new ArrayList<List<Object>>();
            for (Class<?> aClass : classes) {
                if ((aClass.getAnnotation(TableName.class) != null) || (aClass.getAnnotation(Table.class) != null) || (aClass.getAnnotation(Entity.class)!=null)) {
                    List<Object> list1 = new ArrayList<Object>();
                    List<Object> list2 = new ArrayList<Object>();
                    List<Object> list3 = new ArrayList<Object>();
                    list1.add("表字段");
                    if ((aClass.getAnnotation(TableName.class) != null) && isNotEmpty(aClass.getAnnotation(TableName.class).name())){
                        list1.add(aClass.getAnnotation(TableName.class).name());
                    }else if ((aClass.getAnnotation(Table.class) != null) && isNotEmpty(aClass.getAnnotation(Table.class).name())){
                        list1.add(aClass.getAnnotation(Table.class).name());
                    } else {
                        list1.add(aClass.getSimpleName().toLowerCase());
                    }
                    list2.add("字段意思");
                    if ((aClass.getAnnotation(TableName.class) != null) && isNotEmpty(aClass.getAnnotation(TableName.class).message())){
                        list2.add(aClass.getAnnotation(TableName.class).message());
                    } else {
                        list2.add("未写注释");
                    }

                    list3.add("");
                    Field[] declaredFields = aClass.getDeclaredFields();
                    for (Field declaredField : declaredFields) {
                        if ((declaredField.getAnnotation(FieldName.class) != null) || (declaredField.getAnnotation(Column.class) != null)) {
                            if ((declaredField.getAnnotation(FieldName.class) != null) && isNotEmpty(declaredField.getAnnotation(FieldName.class).name())){
                                list1.add(declaredField.getAnnotation(FieldName.class).name());
                            }else if ((declaredField.getAnnotation(Column.class) != null) && isNotEmpty(declaredField.getAnnotation(Column.class).name())){
                                list1.add(declaredField.getAnnotation(Column.class).name());
                            }else {
                                list1.add(apply(declaredField.getName()));
                            }
                            if ((declaredField.getAnnotation(FieldName.class) != null)&& isNotEmpty(declaredField.getAnnotation(FieldName.class).name())){
                                list2.add(declaredField.getAnnotation(FieldName.class).message());
                            } else {
                                list2.add("未写注释");
                            }
                        }else {
                            list1.add(apply(declaredField.getName()));
                            list2.add("未写注释");
                        }

                    }
                    objectList.add(list1);
                    objectList.add(list2);
                    objectList.add(list3);
                }

                OutputStream out = new FileOutputStream(file + "/table.xlsx");

                new ExcelWriterBuilder().excelType(ExcelTypeEnum.XLSX)
                        .file(out)
                        .sheet(1,"配置结果").table(1)
                        .doWrite(objectList);
                out.close();

            }
        } catch (IOException e){
            e.fillInStackTrace();
        }
    }

    private void scanner(List<Class<?>> classes,File dir,String classPath){
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()){
                scanner(classes,file,classPath);
            }else {
                if (!file.getName().endsWith(".class")){
                    continue;
                }
                String path = file.getPath();
                String className = getClassName(path,classPath);
                try {
                    classes.add(Class.forName(className,true,loader));
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private String getClassName(String path,String classPath){
        classPath = (classPath==null?"":classPath);
        String classDir = path.replaceAll("\\\\", "/")
                .replaceAll(classPath.replaceAll("\\\\", "/"), "")
                .replaceAll("/", ".")
                .replaceAll("\\.class", "");
        String className = classDir.substring(1, classDir.length());
        return className;
    }

    private static String apply(String name) {
        if (name == null) {
            return null;
        } else {
            StringBuilder builder = new StringBuilder(name.replace('.', '_'));

            for(int i = 1; i < builder.length() - 1; ++i) {
                if (isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i), builder.charAt(i + 1))) {
                    builder.insert(i++, '_');
                }
            }

            return builder.toString().toLowerCase();
        }
    }

    private static boolean isUnderscoreRequired(char before, char current, char after) {
        return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after);
    }
}

