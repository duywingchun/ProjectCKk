package com.example.projectck;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectck.adapters.ChatAdapter;
import com.example.projectck.models.ChatMessage;
import com.example.projectck.models.Food;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerChat;
    private EditText edtMessage;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private List<Food> foodList = new ArrayList<>();
    private OkHttpClient client;
    // DÁN KEY MỚI
    private final String API_KEY = "AIzaSyCZUNbc5cvNwHerxeJjDZvKyNERiP6L73k";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerChat = findViewById(R.id.recyclerChat);
        edtMessage = findViewById(R.id.edtMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);

        client = new OkHttpClient();
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);

        loadFoodFromFirestore();

        btnSend.setOnClickListener(v -> {
            String userText = edtMessage.getText().toString().trim();
            if (!userText.isEmpty()) {
                sendMessage(userText);
            }
        });
    }

    private void loadFoodFromFirestore() {
        FirebaseFirestore.getInstance().collection("foods").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    foodList = queryDocumentSnapshots.toObjects(Food.class);
                });
    }

    private void sendMessage(String userText) {
        messageList.add(new ChatMessage(userText, ChatMessage.ROLE_USER));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
        edtMessage.setText("");

        // Chuẩn bị danh sách món ăn làm ngữ cảnh
        StringBuilder context = new StringBuilder();
        for (Food f : foodList) context.append(f.getName()).append(", ");

        String prompt = "Thực đơn: " + context.toString() + 
                ". Khách hỏi: " + userText + 
                ". Gợi ý món có trong thực đơn dựa trên khẩu vị (chua, cay, mặn, ngọt...) hoặc tâm trạng (vui, buồn, stress). Trả lời ngắn gọn, tâm lý.";

        callGeminiAPI(prompt, userText);
    }

    private void callGeminiAPI(String prompt, String userText) {
        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject partObj = new JSONObject();
            partObj.put("text", prompt);
            parts.put(partObj);
            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonBody.put("contents", contents);
        } catch (JSONException e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> handleFallback(userText));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String botText = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                        showBotResponse(botText);
                    } catch (Exception e) { runOnUiThread(() -> handleFallback(userText)); }
                } else {
                    // Nếu Google lỗi (404, 403...), dùng AI Nội Bộ ngay
                    runOnUiThread(() -> handleFallback(userText));
                }
            }
        });
    }

    private void showBotResponse(String text) {
        runOnUiThread(() -> {
            messageList.add(new ChatMessage(text, ChatMessage.ROLE_BOT));
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerChat.scrollToPosition(messageList.size() - 1);
        });
    }

    private String lastFoodName = "";

    // AI NỘI BỘ THÔNG MINH: Trực quan, đa dạng và không lặp lại
    private void handleFallback(String userText) {
        String input = userText.toLowerCase();
        
        // 1. Xử lý khi người dùng đồng ý
        if (input.matches(".*(ok|bú|được|hay|tốt|cảm ơn|thanks|vâng|chốt|lấy).*")) {
            String[] okResponses = {
                "✨ Tuyệt vời quá! Đơn hàng món này đã được ghi nhận. Đảm bảo bạn sẽ hài lòng với lựa chọn này ạ!",
                "🔥 Chốt luôn! Bếp nhà mình sẽ bắt tay vào làm ngay cho nóng. Bạn đợi một chút nhé!",
                "✅ Dạ vâng ạ, lựa chọn cực kỳ sáng suốt luôn! Bạn có muốn xem thêm món gì khác không?",
                "🎯 Lựa chọn không thể chuẩn hơn! Mình đã chuyển thông tin cho bếp rồi nhé.",
                "👨‍🍳 Bếp trưởng gửi lời khen cho sự tinh tế của bạn! Món ăn sẽ có mặt ngay thôi.",
                "🚀 Vèo một cái là xong! Đơn hàng của bạn đang được ưu tiên hàng đầu rồi nè.",
                "🥳 Chúc mừng bạn đã tìm được 'chân ái'! Mình sẽ chuẩn bị mọi thứ thật chu đáo.",
                "🌟 Dạ vâng, món này đang là 'ngôi sao' của quán mình đó. Bạn chờ thưởng thức nha!",
                "💯 Điểm 10 cho chất lượng! Món ngon đang trên đường đến với bạn đây.",
                "🔔 Ting ting! Đã nhận yêu cầu. Cảm ơn bạn đã tin tưởng gợi ý của mình nha!"
            };
            showBotResponse(okResponses[(int)(Math.random() * okResponses.length)]);
            return;
        }

        // 2. Phân tích nhu cầu
        boolean isSpecialRequest = input.contains("đặc biệt") || input.contains("khác") || input.contains("thêm");
        boolean isRefusal = input.matches(".*(chán|không thích|dở|đổi|tệ).*");
        boolean isSpicy = input.contains("cay") || input.contains("nồng");
        boolean isSour = input.contains("chua");
        boolean isSweet = input.contains("ngọt");
        boolean isBéo = input.contains("béo") || input.contains("ngậy");
        boolean isSad = input.contains("buồn") || input.contains("chán") || input.contains("tệ");
        boolean isStress = input.contains("stress") || input.contains("mệt") || input.contains("áp lực");
        boolean isHappy = input.contains("vui") || input.contains("sướng");

        List<Food> matches = new ArrayList<>();
        
        // 2.1. Nhận diện từ khóa nguyên liệu/loại món
        boolean wantsChicken = input.contains("gà") || input.contains("chicken");
        boolean wantsBeef = input.contains("bò") || input.contains("beef") || input.contains("steak");
        boolean wantsSeafood = input.contains("hải sản") || input.contains("tôm") || input.contains("mực") || input.contains("seafood") || input.contains("shrimp");
        boolean wantsFish = input.contains("cá") || input.contains("salmon") || input.contains("tuna") || input.contains("sushi");
        boolean wantsNoodle = input.contains("mì") || input.contains("noodle") || input.contains("ramen") || input.contains("pasta");
        boolean wantsPizza = input.contains("pizza");

        for (Food food : foodList) {
            String name = food.getName().toLowerCase();
            String desc = (food.getDescription() != null) ? food.getDescription().toLowerCase() : "";
            String fullInfo = name + " " + desc;

            // Kiểm tra khớp từ khóa nguyên liệu
            boolean keywordMatch = false;
            if (wantsChicken && (fullInfo.contains("gà") || fullInfo.contains("chicken"))) keywordMatch = true;
            if (wantsBeef && (fullInfo.contains("bò") || fullInfo.contains("beef") || fullInfo.contains("steak"))) keywordMatch = true;
            if (wantsSeafood && (fullInfo.contains("hải sản") || fullInfo.contains("tôm") || fullInfo.contains("mực") || fullInfo.contains("seafood"))) keywordMatch = true;
            if (wantsFish && (fullInfo.contains("cá") || fullInfo.contains("salmon") || fullInfo.contains("tuna") || fullInfo.contains("sushi"))) keywordMatch = true;
            if (wantsNoodle && (fullInfo.contains("mì") || fullInfo.contains("noodle") || fullInfo.contains("ramen") || fullInfo.contains("pasta"))) keywordMatch = true;
            if (wantsPizza && fullInfo.contains("pizza")) keywordMatch = true;

            // Nếu người dùng có yêu cầu nguyên liệu cụ thể, ưu tiên lấy những món đó
            if (keywordMatch) {
                matches.add(food);
                continue;
            }

            // Nếu không yêu cầu nguyên liệu hoặc không tìm thấy món khớp nguyên liệu, lọc theo vị/tâm trạng
            if (!wantsChicken && !wantsBeef && !wantsSeafood && !wantsFish && !wantsNoodle && !wantsPizza) {
                if (isSpicy && (fullInfo.contains("cay") || fullInfo.contains("ớt"))) matches.add(food);
                else if (isSour && fullInfo.contains("chua")) matches.add(food);
                else if (isSweet && fullInfo.contains("ngọt")) matches.add(food);
                else if (isBéo && (fullInfo.contains("béo") || fullInfo.contains("ngậy") || fullInfo.contains("phô mai"))) matches.add(food);
                else if ((isSad || isStress) && (fullInfo.contains("ngọt") || fullInfo.contains("béo") || name.contains("burger") || name.contains("pizza"))) matches.add(food);
                else if (isHappy && (name.contains("pizza") || name.contains("steak") || fullInfo.contains("combo"))) matches.add(food);
                else if (input.contains(name)) matches.add(food);
            }
        }

        if (matches.isEmpty()) matches.addAll(foodList);
        
        if (!matches.isEmpty()) {
            Collections.shuffle(matches);
            Food pick = matches.get(0);
            if (pick.getName().equals(lastFoodName) && matches.size() > 1) pick = matches.get(1);
            lastFoodName = pick.getName();

            // 3. Đa dạng hóa lời dẫn (Intros)
            String intro;
            if (isSpecialRequest) {
                String[] specialIntros = {
                    "🌟 Nếu muốn tìm thứ gì đó phá cách hơn, bạn nhất định phải thử:",
                    "💎 Đây là 'vũ khí bí mật' của nhà hàng mình, cực kỳ đặc biệt luôn:",
                    "👑 Để mình giới thiệu cho bạn một 'siêu phẩm' đẳng cấp hơn nhé:",
                    "🌈 Nếu bạn muốn một trải nghiệm bùng nổ vị giác, đừng bỏ qua món này:",
                    "✨ Đây là món ăn mang phong cách rất riêng, đảm bảo không đâu có được:",
                    "🚩 Một sự lựa chọn đầy táo bạo cho những ai muốn đổi mới đây ạ:",
                    "🔭 Hãy cùng khám phá một hương vị hoàn toàn mới lạ qua món này nhé:",
                    "🥇 Nếu những món kia chưa làm bạn hài lòng, thì đây là 'át chủ bài':",
                    "🎭 Hãy thử thay đổi khẩu vị một chút với gợi ý độc đáo này xem sao:",
                    "🕯️ Một món ăn mang đầy sự tinh tế và khác biệt dành riêng cho bạn:"
                };
                intro = specialIntros[(int)(Math.random() * specialIntros.length)];
            } else if (isRefusal) {
                String[] refusalIntros = {
                    "😅 Dạ em hiểu rồi, khẩu vị mỗi người mỗi khác mà. Vậy mình đổi sang món này nhé:",
                    "🙌 Không sao ạ, để mình tìm một hương vị khác hoàn toàn giúp bạn nhé:",
                    "🔄 Đổi gió một chút nha! Món này chắc chắn sẽ khiến bạn thay đổi suy nghĩ đấy:",
                    "💡 Ồ, vậy thì món sau đây có vẻ sẽ 'hợp rơ' với bạn hơn nhiều này:",
                    "🔍 Để mình lọc lại thực đơn và tìm cho bạn một sự lựa chọn tuyệt vời hơn:",
                    "🎨 Đừng lo, thực đơn của mình đa dạng lắm. Thử món này xem sao nhé:",
                    "🧩 Có lẽ mảnh ghép hương vị này mới thực sự dành cho bạn:",
                    "🚿 Hãy quên hương vị cũ đi, món mới này sẽ làm bạn sảng khoái ngay lập tức:",
                    "🚪 Đóng lại món cũ, mình cùng mở ra một sự lựa chọn mới hấp dẫn hơn nha:",
                    "🌈 Dạ vâng, để mình xoay chuyển tình thế bằng một gợi ý cực phẩm khác:"
                };
                intro = refusalIntros[(int)(Math.random() * refusalIntros.length)];
            } else if (isSad || isStress) {
                String[] emotionalIntros = {
                    "💓 Nghe tâm trạng của bạn có vẻ đang cần được 'vỗ về'. Món này sẽ giúp ích đó:",
                    "🤗 Đừng quá áp lực nhé, hãy để món ăn ngon này ôm lấy tâm hồn bạn:",
                    "🍀 Một chút hương vị ngọt ngào/béo ngậy này sẽ làm bạn thấy khá hơn nhiều:",
                    "🔋 Nạp lại năng lượng và tinh thần bằng một món ăn đầy ấm áp nhé:",
                    "🕯️ Hãy để không gian thưởng thức món này xua tan đi nỗi buồn của bạn:",
                    "🍫 Một chút ngọt ngào cho ngày dài mệt mỏi, bạn thấy thế nào?",
                    "🧸 Có những ngày chỉ cần một món ngon là đủ để thấy yêu đời hơn rồi:",
                    "🌤️ Sau cơn mưa trời lại sáng, và món ăn này sẽ là tia nắng của bạn:",
                    "🧘 Thả lỏng cơ thể và tận hưởng hương vị tuyệt vời này để giải tỏa stress nhé:",
                    "🎁 Tặng bạn một niềm vui nho nhỏ thông qua gợi ý món ăn đầy tâm lý này:"
                };
                intro = emotionalIntros[(int)(Math.random() * emotionalIntros.length)];
            } else {
                String[] normalIntros = {
                    "🔔 Gợi ý hàng đầu cho yêu cầu của bạn chính là:",
                    "👨‍🍳 Đầu bếp bên mình vừa gợi ý món này cực kỳ hợp với bạn luôn:",
                    "🚀 Vèo một cái là có ngay món ngon! Bạn xem thử món này nhé:",
                    "🧐 Dựa trên những gì bạn thích, mình tin món này sẽ là lựa chọn số 1:",
                    "💬 Theo khảo sát của các thực khách, đây là món bạn nên thử ngay:",
                    "🏷️ Một 'deal' hời cho vị giác của bạn đây, xem qua thử nha:",
                    "🎈 Hãy để món ăn này làm ngày hôm nay của bạn thêm màu sắc:",
                    "📌 Đây là món ăn được gọi nhiều nhất trong khung giờ này đó:",
                    "🧺 Một chút hương vị quen thuộc nhưng vô cùng lôi cuốn dành cho bạn:",
                    "🪁 Hãy để tâm hồn bạn bay bổng cùng món ăn tuyệt vời sau đây:"
                };
                intro = normalIntros[(int)(Math.random() * normalIntros.length)];
            }

            // 4. Đa dạng hóa lời kết (Closings)
            String[] closings = {
                "Món này mà 'bú' lúc còn nóng là chuẩn bài luôn đó! Chốt chứ bạn? 😉",
                "Bạn thấy món này thế nào? Có muốn mình lên đơn ngay không ạ? ✨",
                "Nghe mô tả thôi đã thấy thèm rồi đúng không? Thử ngay nhé! 🤤",
                "Món này đang là 'trend' của quán đó, bạn đừng bỏ lỡ nha!",
                "Bạn cần mình tư vấn thêm gì về món này không, hay là chốt luôn nhỉ? 📝",
                "Đừng để bụng đói chờ đợi lâu, gật đầu một cái là có món ngay! 🍗",
                "Hương vị này chắc chắn sẽ không làm bạn thất vọng đâu. Bạn nghĩ sao? 🤔",
                "Chỉ một lựa chọn nữa thôi là hạnh phúc ngập tràn rồi. Chốt nha? 😍",
                "Món này cực hợp để thưởng thức cùng bạn bè hoặc người thân đó!",
                "Hãy để món ngon này chăm sóc dạ dày của bạn ngay bây giờ nhé! ✅"
            };
            String close = closings[(int)(Math.random() * closings.length)];

            // Xác định Icon
            String emoji = "🍽️";
            String n = pick.getName().toLowerCase();
            if (n.contains("sushi") || n.contains("ramen") || n.contains("tokbokki")) emoji = "🍱";
            else if (n.contains("burger") || n.contains("chicken") || n.contains("steak") || n.contains("pasta")) emoji = "🍝";
            else if (n.contains("pizza")) emoji = "🍕";
            else if (n.contains("noodle") || n.contains("mì")) emoji = "🍜";

            StringBuilder sb = new StringBuilder();
            sb.append(intro).append("\n\n");
            sb.append(emoji).append(" **").append(pick.getName()).append("**\n");
            
            if (pick.getDescription() != null && !pick.getDescription().isEmpty()) {
                sb.append("✨ *Trải nghiệm:* ").append(pick.getDescription()).append("\n");
            }
            sb.append("\n").append(close);
            
            showBotResponse(sb.toString());
        } else {
            showBotResponse("Dạ, hiện tại quán đang cập nhật thêm món mới. Bạn xem tạm thực đơn trên bảng nhé!");
        }
    }
}
