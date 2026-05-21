package nts.cntt2.quanlychitieu;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class TransactionModel {
    // Dùng @Exclude để Firebase không cố gắng ghi đè trường này vào database
    // vì ID của document là riêng biệt.
    @Exclude
    private String transactionId;

    private String uid;
    private String type;
    private double amount;
    private String category;
    private Timestamp timestamp;
    private String note;

    public TransactionModel() {}

    public TransactionModel(String uid, String type, double amount, String category, Timestamp timestamp, String note) {
        this.uid = uid;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.timestamp = timestamp;
        this.note = note;
    }

    // Getter và Setter cho ID
    @Exclude
    public String getTransactionId() { return transactionId; }
    @Exclude
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    // Các trường dữ liệu khác
    @PropertyName("uid")
    public String getUid() { return uid; }
    @PropertyName("uid")
    public void setUid(String uid) { this.uid = uid; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("amount")
    public double getAmount() { return amount; }
    @PropertyName("amount")
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("category")
    public String getCategory() { return category; }
    @PropertyName("category")
    public void setCategory(String category) { this.category = category; }

    @PropertyName("timestamp")
    public Timestamp getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @PropertyName("note")
    public String getNote() { return note; }
    @PropertyName("note")
    public void setNote(String note) { this.note = note; }
}