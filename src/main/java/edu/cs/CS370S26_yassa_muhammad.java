package edu.cs;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet("/CS370S26_yassa_muhammad")
@MultipartConfig(fileSizeThreshold=1024*1024*10,
               maxFileSize=1024*1024*50,
               maxRequestSize=1024*1024*100)
public class CS370S26_yassa_muhammad extends HttpServlet {

    private static final long serialVersionUID = 205242440643911308L;
    private static final String UPLOAD_DIR = "uploads";

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        String applicationPath = request.getServletContext().getRealPath("");
        String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;
        response.setContentType("text/html;charset=UTF-8");

        File fileSaveDir = new File(uploadFilePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdirs();
        }

        String fileName = "";
        Part filePart = null;

        for (Part part : request.getParts()) {
            fileName = getFileName(part);
            if (!fileName.isEmpty()) {
                filePart = part;
                break;
            }
        }

        // No file uploaded
        if (filePart == null || fileName.isEmpty()) {
            response.getWriter().write("Error: No file uploaded.<br>");
            return;
        }

        // Sanitize filename (remove path traversal)
        fileName = fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        // Validate file type (.txt only)
        if (!fileName.toLowerCase().endsWith(".txt")) {
            response.getWriter().write("Error: Only .txt files are allowed.<br>");
            return;
        }

        // Make filename unique to avoid overwrite
        String uniqueFileName = fileName;
        File file = new File(uploadFilePath, uniqueFileName);

        int count = 1;
        while (file.exists()) {
            String name = fileName.substring(0, fileName.lastIndexOf('.'));
            String ext = fileName.substring(fileName.lastIndexOf('.'));
            uniqueFileName = name + "(" + count + ")" + ext;
            file = new File(uploadFilePath, uniqueFileName);
            count++;
        }
        
        String fullPath = uploadFilePath + File.separator + uniqueFileName;
        filePart.write(fullPath);
        
        // Read file content
        String content = new Scanner(new File(fullPath))
                .useDelimiter("\\Z").next();

        // Escape output to prevent XSS
        String safeContent = escapeHtml(content).replace("\n", "<br>");
        
        try {
            String jdbcURL = "jdbc:mysql://13.220.84.97:3306/csci370?useSSL=true&serverTimezone=UTC";
            String dbUser = "CSCI370_User";
            String dbPassword = "CSCI370";

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(jdbcURL, dbUser, dbPassword);

            String sql = "INSERT INTO uploads (filename, content) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uniqueFileName);
            stmt.setString(2, content);

            stmt.executeUpdate();
            conn.close();

            response.getWriter().write("Stored in DB successfully!<br>");

        } catch (Exception e) {
            // Do NOT expose stack trace to user
            response.getWriter().write("DB Error occurred. Please try again.<br>");
            e.printStackTrace(); // still logs internally
        }
        response.getWriter().write("Result<br>" + safeContent);
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "";
    }

    // Simple HTML escape (XSS protection)
    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}