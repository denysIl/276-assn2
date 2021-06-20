/*
 * Copyright 2002-2014 the original author or authors.
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

package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

@Controller
@SpringBootApplication
public class Main {

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  @RequestMapping("/")
  String index(Map<String, Object> model) {
    return "index";
  }

  @GetMapping(
          path = "/rectangle"
  )
  public String getRectangleForm(Map<String, Object> model){
    Rectangle rectangle = new Rectangle();
    model.put("rectangle", rectangle);
    return "rectangle";
  }


  @PostMapping(
          path = "/rectangle",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String RectangleSubmit(Map<String, Object> model, Rectangle rectangle) throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS rectangles (id serial, name varchar(20), height varchar(20), width varchar(20), color varchar(20))");
      String sql = "INSERT INTO rectangles (name, height, width, color) VALUES ('" + rectangle.getName() + "','" + rectangle.getHeight() + "','" + rectangle.getWidth() + "','" + rectangle.getColor() + "')";
      stmt.executeUpdate(sql);
      return "rectangle";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }

  }


  @GetMapping(
          path = "/rectangles_db"
  )
  public String displayRectangles(Map<String, Object> model){
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();
      ResultSet rs = stmt.executeQuery("SELECT *  FROM rectangles");
      while(rs.next()){
        String name = rs.getString("name");
        String color = rs.getString("color");
        Integer id = rs.getInt("id");
        rectangles.add(new Rectangle(name, color, id));
      }
      model.put("rectangles", rectangles);
      Rectangle rectangle = new Rectangle();
      model.put("rectangle", rectangle);
      return "rectangles_db";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }

  }

  @PostMapping(
          path = "/rectangles_db",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String DeleteRectangle(Map<String, Object> model, Rectangle rectangle) throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      String sql = "DELETE FROM rectangles WHERE id = " + rectangle.getId();
      stmt.executeUpdate(sql);
      return "redirect:/rectangles_db";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }

  }

  @GetMapping(
          path = "rectangles_db/{pid}"
  )
  public String getSpecificRectangle(Map<String, Object> model, @PathVariable String pid){
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      Rectangle rectangle = new Rectangle();
      ResultSet rs = stmt.executeQuery("SELECT * FROM rectangles WHERE id = " + pid);
      if(rs.next()){
        rectangle.setId(rs.getInt("id"));
        rectangle.setName(rs.getString("name"));
        rectangle.setColor(rs.getString("color"));
        rectangle.setHeight(rs.getString("height"));
        rectangle.setWidth(rs.getString("width"));
      }
      model.put("rectangle", rectangle);
      return "display_rectangle";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }



  @RequestMapping("/db")
  String db(Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        output.add("Read from DB: " + rs.getTimestamp("tick"));
      }

      model.put("records", output);
      return "db";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

}
