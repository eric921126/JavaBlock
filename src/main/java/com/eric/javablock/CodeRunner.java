package com.eric.javablock;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class CodeRunner {

    //接收積木拼出來的程式碼字串，編譯並執行後回傳結果
    public static String run(String userCode) {
        // 找出有 main 方法的 class 名稱
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("public class (\\w+)(?=[^}]*public static void main)")
                .matcher(userCode);

        String mainClassName = matcher.find() ? matcher.group(1) : null;
        if (mainClassName == null) return "找不到含有 main 方法的 class";

        // 把其他 public class 拿掉 public，避免一個檔案有多個 public class
        String modifiedCode = userCode.replaceAll(
                "public class (?!" + mainClassName + ")",
                "class "
        );

        try {

            //存檔寫入程式，檔名跟 main class 名稱一致
            Path dir = Files.createTempDirectory("javablock_");
            Path javaFile = dir.resolve(mainClassName + ".java");
            Files.writeString(javaFile, modifiedCode);

            //呼叫編譯器
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) return "找不到編譯器，請確認用 JDK 執行";

            //將錯誤字串傳回
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            int result = compiler.run(null, null, err, javaFile.toString());
            if (result != 0) return "編譯錯誤：\n" + err.toString();

            //執行檔案
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", dir.toString(), mainClassName);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            //超時將程式殺掉
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "執行超過 5 秒，已強制停止";
            }

            String output = new String(process.getInputStream().readAllBytes());

            // 清理暫存檔
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());

            return output;

        } catch (Exception e) {
            return "發生錯誤：" + e.getMessage();
        }
    }
}