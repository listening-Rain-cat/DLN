package org.example.dln;

import org.example.dln.entity.User;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 包名：org.example.dln
 * 类名：DlnApplication
 * 类描述：DLN 后端应用启动入口。
 * 创建人：@author Rain_润
 */
@SpringBootApplication
@MapperScan("org.example.dln.mapper")
public class DlnApplication {

    /**
    * 启动应用。
    */
    public static void main(String[] args) {
        SpringApplication.run(DlnApplication.class, args);
    }

}
