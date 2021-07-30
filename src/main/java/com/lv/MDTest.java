package com.lv;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.crypto.spec.OAEPParameterSpec;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDTest {

    /**
     * 将md文件中线上的图片下载到本地
     * 1 读取本地的md文件
     * 2 将图片链接抽取出来(用正则)
     * 3 发请求 下载图片
     * 4 保存到本地
     */
    public static void main(String[] args) throws Exception {

        String basePackage = MDTest.class.getResource("/").getPath().substring(1);
        File[] files = new File(basePackage + "md").listFiles();
        for (File f : files) {
            //1 读取本地的md文件
            String fileName = f.getName();
            String file = readFile("md/" + fileName, "utf-8");
            //2 将图片链接抽取出来(path这个正则自己写)
            String path = "https://fermhan.oss-cn-qingdao.aliyuncs.com/img/.{14,16}\\.png";
            List<String> picList = getImageURLFromFile(file, path);
            for (String url : picList) {
                file = file.replace(url,
                        "images/" + url.substring(url.lastIndexOf("/") + 1
                                , url.lastIndexOf(".")) + ".png");
            }
            //替换字符串
            new FileOutputStream(fileName + "back").write(file.getBytes("utf-8"));
            //3 发请求 下载图片 4 保存到本地
            getImage(picList);
            System.out.println(fileName + "修改结束");
        }

    }

    public static String readFile(String fileName, String encoding) throws Exception {
        String basePackage = MDTest.class.getResource("/").getPath().substring(1);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(basePackage + fileName), encoding));
        char[] bytes = new char[1024];
        StringBuffer buffer = new StringBuffer();
        int length = 0;
        while ((length = reader.read(bytes)) != -1) {
            buffer.append(new String(bytes, 0, length));
        }
        return buffer.toString();
    }

    public static List<String> getImageURLFromFile(String file, String path) {
        Matcher matcher = Pattern.compile(path).matcher(file);
        List<String> res = new ArrayList<>(20);
        while (matcher.find()) {
            res.add(matcher.group());
        }
        return res;
    }

    public static void getImage(List<String> picList) {
        //1.生成httpclient，相当于该打开一个浏览器
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        //2.创建get请求，相当于在浏览器地址栏输入 网址
        try {
            for (String url : picList) {
                HttpGet request = new HttpGet(url);
                //3.执行get请求，相当于在输入地址栏后敲回车键
                response = httpClient.execute(request);
                //4.判断响应状态为200，进行处理
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    //5.获取响应内容
                    HttpEntity httpEntity = response.getEntity();
                    String fileName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")) + ".png";
                    System.out.println(fileName);
                    httpEntity.writeTo(new FileOutputStream("./images/" + fileName));
                    //每次等半秒 防止反爬策略
                    Thread.sleep(500L);
                } else {
                    //如果返回状态不是200，比如404（页面不存在）等，根据情况做处理，这里略
                    System.out.println("返回状态不是200");
                    System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //6.关闭
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }
    }
}
