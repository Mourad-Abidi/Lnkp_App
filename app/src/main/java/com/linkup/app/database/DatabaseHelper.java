package com.linkup.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.linkup.app.models.*;
import com.linkup.app.security.SecurityUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper for LinkUp Secure Messaging App.
 * Manages SQLite database creation, versioning, and CRUD operations.
 * Updated for Decentralized P2P Architecture.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "LinkUpSecure.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_MESSAGES = "chats";
    private static final String TABLE_MEDIA = "messages_media";
    private static final String TABLE_CALLS = "calls";
    private static final String TABLE_SECURITY = "security";
    private static final String TABLE_SETTINGS = "settings";
    private static final String TABLE_DEVICES = "devices";
    private static final String TABLE_HIDDEN_CHATS = "hidden_chats";
    private static final String TABLE_BLOCKED_USERS = "blocked_users";
    private static final String TABLE_BACKUPS = "backups";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users Table - Added publicKey for P2P identity
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + "userId INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "publicKey TEXT UNIQUE,"
                + "username TEXT,"
                + "phone TEXT,"
                + "email TEXT,"
                + "passwordHash TEXT,"
                + "profilePhoto TEXT,"
                + "accountStatus TEXT,"
                + "createdAt TEXT,"
                + "lastLogin TEXT,"
                + "biometricEnabled INTEGER,"
                + "pinEnabled INTEGER,"
                + "faceUnlockEnabled INTEGER,"
                + "twoFactorEnabled INTEGER" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Chats (Messages) Table - Added sync metadata and messageHash for CRDT
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + "chatId INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "messageHash TEXT UNIQUE,"
                + "senderPublicKey TEXT,"
                + "receiverPublicKey TEXT,"
                + "senderId INTEGER,"
                + "receiverId INTEGER,"
                + "messageText TEXT,"
                + "messageType TEXT,"
                + "timestamp INTEGER,"
                + "deliveryStatus TEXT,"
                + "readStatus TEXT,"
                + "encryptedContent TEXT,"
                + "autoDeleteTime INTEGER,"
                + "isSynced INTEGER DEFAULT 0,"
                + "FOREIGN KEY(senderId) REFERENCES users(userId),"
                + "FOREIGN KEY(receiverId) REFERENCES users(userId)" + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);

        db.execSQL("CREATE TABLE " + TABLE_MEDIA + "(mediaId INTEGER PRIMARY KEY AUTOINCREMENT, chatId INTEGER, filePath TEXT, mediaType TEXT, fileSize INTEGER, duration INTEGER, quality TEXT, FOREIGN KEY(chatId) REFERENCES chats(chatId))");
        db.execSQL("CREATE TABLE " + TABLE_CALLS + "(callId INTEGER PRIMARY KEY AUTOINCREMENT, callerId INTEGER, receiverId INTEGER, callType TEXT, callStatus TEXT, duration INTEGER, timestamp INTEGER, FOREIGN KEY(callerId) REFERENCES users(userId), FOREIGN KEY(receiverId) REFERENCES users(userId))");
        db.execSQL("CREATE TABLE " + TABLE_SECURITY + "(securityId INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER, passwordChangedDate TEXT, activePin TEXT, activeFingerprint INTEGER, activeFaceUnlock INTEGER, active2FA INTEGER, trustedDevices TEXT, sessionHistory TEXT, emergencyLockStatus INTEGER, FOREIGN KEY(userId) REFERENCES users(userId))");
        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + "(settingId INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER, theme TEXT, fontSize TEXT, wallpaper TEXT, notificationsEnabled INTEGER, vibrationEnabled INTEGER, ringtone TEXT, screenshotProtection INTEGER, readReceipts INTEGER, autoDownload TEXT, mediaQuality TEXT, FOREIGN KEY(userId) REFERENCES users(userId))");
        db.execSQL("CREATE TABLE " + TABLE_DEVICES + "(deviceId INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER, deviceName TEXT, deviceModel TEXT, loginDate TEXT, ipAddress TEXT, sessionStatus TEXT, FOREIGN KEY(userId) REFERENCES users(userId))");
        db.execSQL("CREATE TABLE " + TABLE_HIDDEN_CHATS + "(hiddenChatId INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER, protectedChatId INTEGER, unlockMethod TEXT, FOREIGN KEY(userId) REFERENCES users(userId), FOREIGN KEY(protectedChatId) REFERENCES chats(chatId))");
        db.execSQL("CREATE TABLE " + TABLE_BLOCKED_USERS + "(blockId INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER, blockedUserId INTEGER, blockedDate TEXT, FOREIGN KEY(userId) REFERENCES users(userId), FOREIGN KEY(blockedUserId) REFERENCES users(userId))");
        db.execSQL("CREATE TABLE " + TABLE_BACKUPS + "(backupId INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER, backupDate TEXT, backupPath TEXT, backupType TEXT, FOREIGN KEY(userId) REFERENCES users(userId))");

        db.execSQL("CREATE INDEX idx_user_pubkey ON users(publicKey)");
        db.execSQL("CREATE INDEX idx_msg_hash ON chats(messageHash)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN publicKey TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN messageHash TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN senderPublicKey TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN receiverPublicKey TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN isSynced INTEGER DEFAULT 0");
        }
    }

    public void registerLocalIdentity(String publicKey, String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("publicKey", publicKey);
        values.put("username", username);
        values.put("accountStatus", "ACTIVE");
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public long addPendingMessage(String receiverPubKey, String text) {
        SQLiteDatabase db = this.getWritableDatabase();
        String myPubKey = SecurityUtils.getPublicKey();

        ContentValues values = new ContentValues();
        values.put("senderPublicKey", myPubKey);
        values.put("receiverPublicKey", receiverPubKey);
        values.put("messageText", SecurityUtils.encrypt(text));
        values.put("timestamp", System.currentTimeMillis());
        values.put("isSynced", 0);
        
        String hashData = myPubKey + receiverPubKey + text + values.getAsLong("timestamp");
        values.put("messageHash", SecurityUtils.hashPassword(hashData));

        return db.insert(TABLE_MESSAGES, null, values);
    }

    public List<MessageModel> getUnsyncedMessages() {
        List<MessageModel> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, null, "isSynced=0", null, null, null, "timestamp ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                MessageModel msg = new MessageModel();
                msg.setMessage(SecurityUtils.decrypt(cursor.getString(cursor.getColumnIndexOrThrow("messageText"))));
                msg.setSenderName(cursor.getString(cursor.getColumnIndexOrThrow("senderPublicKey")));
                // We'd normally populate more fields here
                messages.add(msg);
            }
            cursor.close();
        }
        return messages;
    }

    public void markAsSynced(String messageHash) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isSynced", 1);
        db.update(TABLE_MESSAGES, values, "messageHash=?", new String[]{messageHash});
    }
}
