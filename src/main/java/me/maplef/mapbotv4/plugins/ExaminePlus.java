package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.exceptions.GroupNotAllowedException;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class ExaminePlus {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    private final Long examineGroup = config.getLong("examine-group");
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    final String MYSQL_HOST = config.getString("survey-mysql-host");
    final String PORT = config.getString("survey-mysql-port");
    final String DB_NAME = config.getString("survey-mysql-database");
    final String DB_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + PORT + "/" + DB_NAME;
    final String USERNAME = config.getString("survey-mysql-username");
    final String PASSWORD = config.getString("survey-mysql-password");
    private final Connection c = connect();
    final String ID = config.getString("survey-project-id");
    static final Bot bot = BotOperator.getBot();

//     @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        if (!Objects.equals(groupID, examineGroup))
            throw new GroupNotAllowedException();
        String link;
        long qq;
        try {
            qq = Long.parseLong(args[0].contentToString());
            link = args[1].contentToString();
        } catch (Exception e) {
            throw new InvalidSyntaxException();
        }
        try {
            String order = "CREATE TABLE IF NOT EXISTS EXAMINE_PLUS (" +
                    "    QQ      TEXT    NOT NULL," +
                    "    MSG     TEXT    NOT NULL" +
                    ");";
            new DatabaseOperator().executeCommand(order);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            String sql = "SELECT answer, exam_score, create_at FROM t_answer WHERE project_id = ?";
//            PreparedStatement stmt = c.prepareStatement(sql);
//            stmt.setString(1, ID);
//            ResultSet rs = stmt.executeQuery(sql);
//            List<List<Object>> results = new ArrayList<>();
//            while (rs.next()) {
//                String answer = rs.getString("answer");
//                int score = rs.getInt("exam_score");
//                Timestamp createdAt = rs.getTimestamp("create_at");
//                List<Object> row = new ArrayList<>(); // Create a new list to store the current row of data
//                row.add(answer);
//                row.add(score);
//                row.add(createdAt);
//                results.add(row);
//            }
//            c.close();
//            results = processAnswer(qq, results);
//            results = processResults(results);
//            return new MessageChainBuilder().append(results.toString()).build();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
        bot.getGroup(examineGroup).sendMessage("请点击链接进行审核：" + link);
        String order = String.format("INSERT INTO EXAMINE_PLUS (QQ, MSG) VALUES ('%s', '%s');", qq, "请点击链接进行审核：" + link);
        try {
            new DatabaseOperator().executeCommand(order);
        } catch (SQLException e) {
            e.printStackTrace();
            return new MessageChainBuilder().append("出现错误，请查看控制台报错信息").build();
        }
        return null;
    }

    public static List<List<Object>> processAnswer(long qq, List<List<Object>> results) {
        try {
            List<List<Object>> processedResults = new ArrayList<>(); // Create a new list to store the processed results
            for (List<Object> row : results) {
                String answerJson = (String) row.get(0); // Get the answer JSON string from the current row
                JSONObject answerObj = new JSONObject(answerJson); // Parse the JSON string into a JSONObject
                JSONObject uudaObj = answerObj.optJSONObject("yef6"); // Get the "yef6" object from the answer JSON
                if (uudaObj != null && uudaObj.has("uuda") && uudaObj.getString("uuda").equals(String.valueOf(qq))) {
                    processedResults.add(row); // If the "uuda" value matches the given qq, add the current row to the processed results
                }
            }
            return processedResults; // Return the processed results

        } catch (ClassCastException ignored) {}
        return null;
    }

    public List<List<Object>> processResults(List<List<Object>> results) {
        List<List<Object>> processedResults = new ArrayList<>();

        // 如果只剩下一个元素，则直接保留
        if (results.size() == 1) {
            processedResults.addAll(results);
        } else {
            // 将每个元素转换为Map
            List<Map<String, String>> mapList = new ArrayList<>();
            for (List<Object> row : results) {
                Map<String, String> map = new HashMap<>();
                map.put("answer", (String) row.get(0));
                map.put("score", (String) row.get(1));
                map.put("createAt", (String) row.get(2));
                mapList.add(map);
            }

            // 按照createAt排序，时间靠后的排在最前面
            mapList.sort((o1, o2) -> {
                String createAt1 = o1.get("createAt");
                String createAt2 = o2.get("createAt");
                return createAt2.compareTo(createAt1);
            });

            // 只保留时间最靠后的元素，将其他元素删除
            processedResults.add(Arrays.asList(mapList.get(0).get("answer"), mapList.get(0).get("score"), mapList.get(0).get("createAt")));
        }
        return processedResults;
    }


//    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("拉取", ExaminePlus.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("拉取", "暂无");

        info.put("name", "拉取");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "LQSnow");
        info.put("description", "暂无");
        info.put("version", "1.0");

        return null;  // disabled
    }

    private Connection connect() {
        Connection conn = null;
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    public Connection getConnect() {
        return c;
    }

    public void executeCommand(String sqlCommand) throws SQLException {
        try (Statement stmt = c.createStatement()) {
            stmt.execute(sqlCommand);
        }
    }
}
