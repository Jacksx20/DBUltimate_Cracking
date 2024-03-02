package main;
 
import org.jkiss.lm.LMEncryption;
import org.jkiss.lm.LMException;
import org.jkiss.lm.LMLicense;
import org.jkiss.lm.LMLicenseType;
import org.jkiss.lm.LMProduct;
import org.jkiss.lm.LMProductType;
import org.jkiss.lm.LMUtils;
import org.jkiss.utils.Base64;
 
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
 
/**
 * 本代码依赖 2 个 jar 包
 * <ul>
 *     <li>${DBEAVER_ECLIPSE_HOME}/plugins/org.jkiss.utils_**.jar</li>
 *     <li>${DBEAVER_ECLIPSE_HOME}/plugins/org.jkiss.lm_**.jar</li>
 * </ul>
 */
public class DBCrack {
    private static String productID = "dbeaver-ue";
    private static String productVersion = "23.3.0";
    private static String ownerID = "10006";
    private static String ownerCompany = "DBeaver Corporation";
    private static String ownerName = "Ultimate User Jack20";
    private static String ownerEmail = "Jack20@dbeaver.com";
 
    private static File privateKeyFile = new File(new File(System.getProperty("user.home"), ".jkiss-lm"), "private-key.txt");
    private static File publicKeyFile = new File(new File(System.getProperty("user.home"), ".jkiss-lm"), "public-key.txt");
    private static File licenseFile = new File(new File(System.getProperty("user.home"), ".jkiss-lm"), "license.txt");
    /**
     * 使用该文件，需要替换 jar 包中的 key 文件(请保留原始 jar 包), 并在 dbeaver.ini 添加参数-Dlm.debug.mode=true
     * <p>
     * 如果修改 jar 包中的 plugin.xml 的 product 节点添加 <property name="lmServerURL" value="http://localhost:7879"/> 需要一个授权服务器
     * </p>
     */
    private static File jarPublicKey = new File(new File(System.getProperty("user.home"), ".jkiss-lm"), "dbeaver-ue-public.key");
 
    private static final String DBEAVER_ECLIPSE_HOME = "D:\\APP\\DBeaverUltimate";
 
    public static void main(String[] args) throws Exception {
        String dbeaverPluginDir = DBEAVER_ECLIPSE_HOME + File.separator + "plugins";
        // 需要替换公钥的文件 (com.dbeaver.app.ultimate_**.jar)
        String replaceJarFile = "com\\.dbeaver\\.app\\.ultimate_.+\\.jar";
        List<File> matchingFiles = findFilesByPattern(dbeaverPluginDir, replaceJarFile);
        if (matchingFiles.size() != 1) {
            throw new IOException("未找到需要替换的 jar 文件");
        }
        String dbeaverJar = matchingFiles.get(0).getAbsolutePath();
        generateKeyLicenseAndPatch(dbeaverJar, "keys/dbeaver-ue-public.key");
        String iniFile = DBEAVER_ECLIPSE_HOME + File.separator + "dbeaver.ini";
        updateEclipseIniFile(iniFile, "-Dlm.debug.mode=true");
    }
 
    /**
     * 在文件中追加一行配置文本(已经存在则跳过)
     *
     * [url=home.php?mod=space&uid=952169]@Param[/url] iniFile
     * @param configLine
     * @throws IOException
     */
    public static void updateEclipseIniFile(String iniFile, String configLine) throws IOException {
        boolean targetLineFound = false;
 
        // 读取 ini 文件
        File file = new File(iniFile);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
 
        String line;
        StringBuilder content = new StringBuilder();
        while ((line = br.readLine()) != null) {
            content.append(line).append(System.lineSeparator());
            if (line.trim().equals(configLine)) {
                targetLineFound = true;
            }
        }
        br.close();
 
        // 如果未找到目标行，在内容的末尾追加文本
        if (!targetLineFound) {
            content.append(configLine).append(System.lineSeparator());
 
            // 将更新的内容写回 ini 文件
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content.toString());
            bw.close();
        }
    }
 
    /**
     * 通过正则表达匹配，搜索匹配的文件
     *
     * @param directoryPath 目录
     * @param pattern       文件匹配正则
     * @return
     */
    public static List<File> findFilesByPattern(String directoryPath, String pattern) {
        List<File> matchingFiles = new ArrayList<>();
        File directory = new File(directoryPath);
 
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.matches(pattern));
 
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        matchingFiles.add(file);
                    }
                }
            }
        } else {
            System.out.println("无效的目录，或者找不到目录路径！");
        }
 
        return matchingFiles;
    }
 
    /**
     * 生成公钥、私钥、授权码，并替换文件
     *
     * @param jarFilePath   需要替换公钥的 jar 文件
     * @param filePathInJar jar 包中的公钥文件路径
     * @throws LMException
     * @throws IOException
     */
    public static void generateKeyLicenseAndPatch(String jarFilePath, String filePathInJar) throws LMException, IOException {
        // 从 LMMain.generateKeyPair() 复制的代码
        KeyPair keyPair = LMEncryption.generateKeyPair(2048);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        System.out.println("\n--- PUBLIC KEY ---");
        String publicKeyContent = Base64.splitLines(Base64.encode(publicKey.getEncoded()), 76);
        System.out.println(publicKeyContent);
        writeFileToPath(publicKeyContent, publicKeyFile, "公钥");
        System.out.println("\n--- PRIVATE KEY ---");
        String privateKeyContent = Base64.splitLines(Base64.encode(privateKey.getEncoded()), 76);
        System.out.println(privateKeyContent);
        writeFileToPath(publicKeyContent, privateKeyFile, "私钥");
        writeFileToPath(publicKeyContent, jarPublicKey, "公钥");
 
        // 替换 jar 包中的 public key 文件
        replaceFileInJar(jarFilePath, filePathInJar, jarPublicKey.getAbsolutePath());
 
        // 从 LMMain.encryptLicense() 复制的代码
        LMProduct TEST_PRODUCT = new LMProduct("dbeaver-ue", "DB", "DBeaver Ultimate", "DBeaver Ultimate Edition", "23.3", LMProductType.DESKTOP, new Date(), new String[0]);
        String licenseID = LMUtils.generateLicenseId(TEST_PRODUCT);
        LMLicense license = new LMLicense(licenseID, LMLicenseType.ULTIMATE, new Date(), new Date(), (Date) null, LMLicense.FLAG_UNLIMITED_SERVERS, productID, productVersion, ownerID, ownerCompany, ownerName, ownerEmail);
        byte[] licenseData = license.getData();
        byte[] licenseEncrypted = LMEncryption.encrypt(licenseData, privateKey);
 
        String licenseContent = Base64.splitLines(Base64.encode(licenseEncrypted), 76);
        System.out.println("\n--- LICENSE ---");
        System.out.println(licenseContent);
        System.out.println("--- 处理完成，请打开软件使用以上授权码（已复制到剪切板） ---");
        copyToClipboard(licenseContent);
        writeFileToPath(licenseContent, licenseFile, "授权码");
    }
 
    /**
     * 写入并覆盖文件内容
     *
     * @param content  写入覆盖的内容
     * @param filePath 要写入的文件路径
     * @param tip      提示
     */
    public static void writeFileToPath(String content, File filePath, String tip) {
        try {
            Path path = Paths.get(filePath.getAbsolutePath());
            Files.write(path, content.getBytes());
            System.out.println(String.format("[%s内容]已成功写入文件：%s", tip, path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    /**
     * 替换 jar 包中的指定路径的文件为新文件
     *
     * @param jarFilePath
     * @param targetFile
     * @param newFile
     * @throws IOException
     */
    public static void replaceFileInJar(String jarFilePath, String targetFile, String newFile) throws IOException {
        File tempFile = File.createTempFile("temp-file", ".tmp");
        File tempJarFile = File.createTempFile("temp-jar", ".tmp");
 
        boolean replaceSuccess = false;
        try (JarFile jarFile = new JarFile(new File(jarFilePath));
             JarOutputStream tempJarOutputStream = new JarOutputStream(new FileOutputStream(tempJarFile))) {
 
            // Copy the existing entries to the temp jar, excluding the entry to be replaced
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
 
                if (!entry.getName().equals(targetFile)) {
                    tempJarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        copyStream(inputStream, tempJarOutputStream);
                    }
                }
            }
 
            // 将新文件添加到临时jar
            tempJarOutputStream.putNextEntry(new JarEntry(targetFile));
            try (InputStream newFileStream = new FileInputStream(newFile)) {
                copyStream(newFileStream, tempJarOutputStream);
            }
            replaceSuccess = true;
        } finally {
            // 将原始 jar 文件替换为临时 jar 文件
            File originalJarFile = new File(jarFilePath);
            if (originalJarFile.delete() && tempJarFile.renameTo(originalJarFile) && replaceSuccess) {
                System.out.println(String.format("jar包中的文件 [%s] 替换成功！", targetFile));
            } else {
                System.out.println(String.format("jar包中的文件 [%s] 替换失败！", targetFile));
            }
 
            tempFile.delete();
            tempJarFile.delete();
        }
    }
 
    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
 
    /**
     * 将指定字符串复制到剪切板
     *
     * @param text
     */
    public static void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}