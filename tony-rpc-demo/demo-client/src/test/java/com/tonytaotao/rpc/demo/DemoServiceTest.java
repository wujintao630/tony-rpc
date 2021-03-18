package com.tonytaotao.rpc.demo;

import com.tonytaotao.rpc.demo.api.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-tonyrpc.xml"})
public class DemoServiceTest {

    @Resource(name = "demoService")
    private DemoService demoService;

    @Test
    public void helloTest() {
       String result = demoService.hello();

        System.out.println(result);
    }

}
