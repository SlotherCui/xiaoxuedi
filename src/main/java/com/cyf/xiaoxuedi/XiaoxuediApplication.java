package com.cyf.xiaoxuedi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = {"com.cyf.xiaoxuedi"})
@MapperScan("com.cyf.xiaoxuedi.DAO")
public class XiaoxuediApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaoxuediApplication.class, args);
    }

}
