package server.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspC;
import server.catalina.Context;

import java.io.File;
import java.util.Arrays;

/**
* 编译JSP文件所需的工具类
* @author cn-wumo
* @since 2021/4/22
*/
public class JspUtil {

    /**
    * 编译指定的JSP文件
    * @param context JSP文件所属的web应用程序容器
 	* @param file 待编译的JSP文件
    * @author cn-wumo
    * @since 2021/4/23
    */
    public static void compileJsp(Context context, File file) throws JasperException{
        String subFolder;
        String path = context.getPath();
        if ("/".equals(path))   // "_"为JavaServer-Alpha的默认web应用程序容器名称
            subFolder = "_";
        else
            subFolder = StrUtil.subAfter(path, '/', false); //获取web应用的Context容器名

        String workPath = new File(Constant.workFolder, subFolder).getAbsolutePath() + File.separator;

        String[] args = new String[] {
                "-webapp", context.getDocBase().toLowerCase(),
                "-d", workPath.toLowerCase(),
                "-compile",
        };

        JspC jspc = new JspC(); //创建jspClass文件
        jspc.setArgs(args);
        jspc.execute(file);
    }


    /**
    * 将jsp文件名转化成合法的java文件名
    * @param identifier 待转化的jsp文件名
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/23
    */
    public static String makeJavaIdentifier(String identifier) {
        StringBuilder modifiedIdentifier = new StringBuilder(identifier.length());

        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch) && (ch != '_')) {    //合法且不等于'_'
                modifiedIdentifier.append(ch);
            } else if (ch == '.') {
                modifiedIdentifier.append('_');
            } else {
                modifiedIdentifier.append(mangleChar(ch));  //将不合法字符转化成唯一且可还原的字符串
            }
        }
        if (isJavaKeyword(modifiedIdentifier.toString())) {
            modifiedIdentifier.append('_');
        }
        return modifiedIdentifier.toString();
    }

    /**
    * 将字符转化成"_xxxx"模式的String
    * @param ch 待转化的字符
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/23
    */
    public static String mangleChar(char ch) {
        char[] result = new char[5];
        result[0] = '_';
        result[1] = Character.forDigit((ch >> 12) & 0xf, 16);
        result[2] = Character.forDigit((ch >> 8) & 0xf, 16);
        result[3] = Character.forDigit((ch >> 4) & 0xf, 16);
        result[4] = Character.forDigit(ch & 0xf, 16);
        return new String(result);
    }

    /**
    * 判断字符串是否是Java的关键字
    * @param key 待判断的字符串
    * @return boolean
    * @author cn-wumo
    * @since 2021/4/23
    */
    public static boolean isJavaKeyword(String key) {
        return Arrays.asList(Constant.javaKeywords).contains(key);
    }

    /**
    * 获取JSPServlet的Class路径
    * @param uri 用户访问的uri
 	* @param subFolder web应用程序所处的workFolder
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/23
    */
    public static String getServletClassPath(String uri, String subFolder) {
        return getServletPath(uri, subFolder) + ".class";
    }

    /**
     * 获取JSPServlet的Java路径
     * @param uri 用户访问的uri
     * @param subFolder web应用程序所处的workFolder
     * @return java.lang.String
     * @author cn-wumo
     * @since 2021/4/23
     */
    public static String getServletJavaPath(String uri, String subFolder) {
        return getServletPath(uri, subFolder) + ".java";
    }

    /**
     * 获取JSPServlet的Class的全称
     * @param uri 用户访问的uri
     * @param subFolder web应用程序所处的workFolder
     * @return java.lang.String
     * @author cn-wumo
     * @since 2021/4/23
     */
    public static String getJspServletClassName(String uri, String subFolder) {
        File tempFile = FileUtil.file(Constant.workFolder, subFolder);
        String tempPath = tempFile.getAbsolutePath() + File.separator;  //workFolder的路径
        String servletPath = getServletPath(uri, subFolder);    //servlet文件的路径

        String jsServletClassPath = StrUtil.subAfter(servletPath, tempPath, false); //截取workFolder下的servlet文件相对路径
        return StrUtil.replace(jsServletClassPath, File.separator, ".");    //将servlet文件的相对路径转化为servlet全称
    }

    /**
     * 获取JSPServlet的路径
     * @param uri 用户访问的uri
     * @param subFolder web应用程序所处的workFolder
     * @return java.lang.String
     * @author cn-wumo
     * @since 2021/4/23
     */
    public static String getServletPath(String uri, String subFolder) {
        String tempPath = "org/apache/jsp/" + uri;

        File tempFile = FileUtil.file(Constant.workFolder, subFolder, tempPath);

        String fileNameOnly = tempFile.getName();
        String classFileName = JspUtil.makeJavaIdentifier(fileNameOnly);
        
        File servletFile = new File(tempFile.getParent(), classFileName);
        return servletFile.getAbsolutePath();
    }
}
