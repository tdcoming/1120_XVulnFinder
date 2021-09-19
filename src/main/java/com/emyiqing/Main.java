package com.emyiqing;

import com.beust.jcommander.JCommander;
import com.emyiqing.core.xss.ServletXSS;
import com.emyiqing.dto.Result;
import com.emyiqing.input.Command;
import com.emyiqing.input.Logo;
import com.emyiqing.util.FileUtil;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        Logo.PrintLogo();
        Command command = new Command();
        JCommander jc = JCommander.newBuilder().addObject(command).build();
        jc.parse(args);
        if (command.help) {
            jc.usage();
        }
        String code;
        try {
            code = FileUtil.readFile(command.filename);
        } catch (IOException e) {
            logger.error("read code error");
            return;
        }

        CompilationUnit compilationUnit = StaticJavaParser.parse(code);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        Future<List<Result>> task = executor.submit(()-> ServletXSS.check(compilationUnit));

        try {
            List<Result> result = task.get();
            result.stream().filter(Result::isSuccess).forEach(r -> {
                String format = String.format("class: %s method %s keyword: %s",
                        r.getClassName(), r.getMethodName(),r.getKeyword());
                logger.info(format);
            });
            executor.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
