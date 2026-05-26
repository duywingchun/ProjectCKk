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
    // DÁN KEY MỚI TỪ AI STUDIO (CÁI CÓ ĐUÔI 8I0D) VÀO ĐÂY
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

    // AI NỘI BỘ THÔNG MINH: Biết đối đáp và không lặp lại
    private void handleFallback(String userText) {
        String input = userText.toLowerCase();
        
        // 1. Xử lý khi người dùng đồng ý (ok, bú luôn, được...)
        if (input.matches(".*(ok|bú|được|hay|tốt|cảm ơn|thanks|vâng).*")) {
            showBotResponse("Dạ tuyệt vời! Chốt đơn món này nhé. Chúc bạn ngon miệng ạ! Bạn có muốn tìm thêm món gì nữa không?");
            return;
        }

        // 2. Xử lý khi người dùng chê hoặc muốn đổi món (chán, không thích, đổi...)
        boolean isRefusal = input.matches(".*(chán|không thích|dở|khác|đổi|tệ).*");
        
        List<Food> matches = new ArrayList<>();
        boolean isSpicy = input.contains("cay");
        boolean isSour = input.contains("chua");
        boolean isSweet = input.contains("ngọt");
        boolean isSad = input.contains("buồn") || input.contains("chán") || input.contains("tệ");

        for (Food food : foodList) {
            String name = food.getName().toLowerCase();
            String desc = (food.getDescription() != null) ? food.getDescription().toLowerCase() : "";
            
            if (isSpicy && (name.contains("cay") || desc.contains("cay"))) matches.add(food);
            if (isSour && (name.contains("chua") || desc.contains("chua"))) matches.add(food);
            if (isSweet && (name.contains("ngọt") || desc.contains("ngọt"))) matches.add(food);
        }

        // Nếu không khớp từ khóa nào, lấy toàn bộ danh sách để chọn ngẫu nhiên
        if (matches.isEmpty()) matches.addAll(foodList);
        
        if (!matches.isEmpty()) {
            Collections.shuffle(matches);
            Food pick = matches.get(0);
            
            // Tránh lặp lại món vừa gợi ý
            if (pick.getName().equals(lastFoodName) && matches.size() > 1) {
                pick = matches.get(1);
            }
            lastFoodName = pick.getName();

            String response;
            if (isRefusal) {
                response = "Dạ em xin lỗi ạ! Vậy mình đổi sang món '" + pick.getName() + "' nhé? Món này đảm bảo hương vị khác biệt luôn ạ!";
            } else if (isSad) {
                response = "Nghe tâm trạng của bạn có vẻ không tốt... Hay là thử món '" + pick.getName() + "' cho đời thêm tươi sáng nhỉ?";
            } else if (isSpicy) {
                response = "Nếu bạn thích cảm giác bùng nổ, hãy thử ngay '" + pick.getName() + "' nhé, vị cay nồng đúng điệu luôn!";
            } else {
                List<String> templates = new ArrayList<>();
                templates.add("Dạ, quán mình có món '" + pick.getName() + "' đang cực kỳ 'đắt khách', bạn dùng thử không?");
                templates.add("Hay là thử món '" + pick.getName() + "' ạ? Món này là 'best seller' của quán mình đó!");
                templates.add("Mời bạn trải nghiệm món '" + pick.getName() + "' nhé, đầu bếp bên mình vừa mới ra lò xong, thơm lắm!");
                Collections.shuffle(templates);
                response = templates.get(0);
            }
            showBotResponse(response);
        } else {
            showBotResponse("Dạ, hiện tại quán đang cập nhật thêm món mới. Bạn xem tạm thực đơn trên bảng nhé!");
        }
    }
}
