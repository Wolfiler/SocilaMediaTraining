db.createCollection("notifications");
db.createCollection("notification_preferences");

db.notifications.createIndex({ userId: 1, status: 1, createdAt: -1 });
db.notifications.createIndex({ createdAt: 1 }, { expireAfterSeconds: 604800 });

db.getSiblingDB('admin').auth('root', 'password');
db.createUser({
    user: 'appUser',
    pwd: 'appPassword123',
    roles: [
        { role: 'readWrite', db: 'sm_notification_service_db' }
    ]
});
