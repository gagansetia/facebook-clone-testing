package com.facebookclone;

import java.time.LocalDateTime;
import java.util.*;

/** Small in-memory Facebook-style application used as the system under test. */
public class FacebookService {
    public enum Privacy { PUBLIC, FRIENDS, ONLY_ME }
    public record User(String username, String email, String password, String displayName, String bio,
                       Privacy privacy, boolean active) { }
    public record Post(long id, String author, String text, Privacy privacy, LocalDateTime createdAt) { }
    public record Message(String from, String to, String text, LocalDateTime sentAt) { }

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Set<String>> friends = new HashMap<>();
    private final Map<String, Set<String>> incomingRequests = new HashMap<>();
    private final Map<String, Set<String>> blocked = new HashMap<>();
    private final Map<Long, Post> posts = new LinkedHashMap<>();
    private final Map<Long, Set<String>> likes = new HashMap<>();
    private final Map<Long, List<String>> comments = new HashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private final Map<String, List<String>> notifications = new HashMap<>();
    private long nextPostId = 1;

    public boolean register(String username, String email, String password, String displayName) {
        if (!validUsername(username) || !validEmail(email) || !validPassword(password) || blank(displayName)
                || users.containsKey(username.toLowerCase()) || emailTaken(email)) return false;
        String key = username.toLowerCase();
        users.put(key, new User(key, email.toLowerCase(), password, displayName.trim(), "", Privacy.PUBLIC, true));
        friends.put(key, new HashSet<>()); incomingRequests.put(key, new HashSet<>());
        blocked.put(key, new HashSet<>()); notifications.put(key, new ArrayList<>());
        return true;
    }

    public boolean login(String username, String password) {
        User user = users.get(normalize(username));
        return user != null && user.active() && Objects.equals(user.password(), password);
    }

    public boolean resetPassword(String email, String newPassword) {
        if (!validPassword(newPassword)) return false;
        for (Map.Entry<String, User> entry : users.entrySet()) {
            if (entry.getValue().email().equalsIgnoreCase(email)) {
                User u = entry.getValue();
                entry.setValue(new User(u.username(), u.email(), newPassword, u.displayName(), u.bio(), u.privacy(), u.active()));
                return true;
            }
        }
        return false;
    }

    public boolean updateProfile(String username, String displayName, String bio, Privacy privacy) {
        User u = users.get(normalize(username));
        if (u == null || blank(displayName) || bio == null || bio.length() > 160 || privacy == null) return false;
        users.put(u.username(), new User(u.username(), u.email(), u.password(), displayName.trim(), bio.trim(), privacy, u.active()));
        return true;
    }

    public boolean sendFriendRequest(String from, String to) {
        String a = normalize(from), b = normalize(to);
        if (!validPair(a, b) || friends.get(a).contains(b) || incomingRequests.get(b).contains(a)
                || blocked.get(a).contains(b) || blocked.get(b).contains(a)) return false;
        incomingRequests.get(b).add(a); notify(b, a + " sent you a friend request"); return true;
    }
    public boolean acceptFriendRequest(String receiver, String sender) {
        String a = normalize(receiver), b = normalize(sender);
        if (!validPair(a, b) || !incomingRequests.get(a).remove(b)) return false;
        friends.get(a).add(b); friends.get(b).add(a); notify(b, a + " accepted your friend request"); return true;
    }
    public boolean rejectFriendRequest(String receiver, String sender) {
        String a = normalize(receiver), b = normalize(sender);
        return validPair(a, b) && incomingRequests.get(a).remove(b);
    }
    public boolean unfriend(String first, String second) {
        String a = normalize(first), b = normalize(second);
        return validPair(a, b) && friends.get(a).remove(b) && friends.get(b).remove(a);
    }
    public boolean block(String blocker, String target) {
        String a = normalize(blocker), b = normalize(target);
        if (!validPair(a, b)) return false;
        blocked.get(a).add(b); friends.get(a).remove(b); friends.get(b).remove(a);
        incomingRequests.get(a).remove(b); incomingRequests.get(b).remove(a); return true;
    }

    public long createPost(String author, String text, Privacy privacy) {
        String a = normalize(author);
        if (!users.containsKey(a) || blank(text) || text.length() > 500 || privacy == null) return -1;
        long id = nextPostId++; posts.put(id, new Post(id, a, text.trim(), privacy, LocalDateTime.now()));
        likes.put(id, new HashSet<>()); comments.put(id, new ArrayList<>()); return id;
    }
    public boolean editPost(String author, long id, String text) {
        Post p = posts.get(id); if (p == null || !p.author().equals(normalize(author)) || blank(text) || text.length() > 500) return false;
        posts.put(id, new Post(id, p.author(), text.trim(), p.privacy(), p.createdAt())); return true;
    }
    public boolean deletePost(String author, long id) {
        Post p = posts.get(id); if (p == null || !p.author().equals(normalize(author))) return false;
        posts.remove(id); likes.remove(id); comments.remove(id); return true;
    }
    public boolean likePost(String user, long id) {
        String u = normalize(user); Post p = posts.get(id);
        if (p == null || !users.containsKey(u) || !canView(u, p)) return false;
        boolean added = likes.get(id).add(u); if (added && !u.equals(p.author())) notify(p.author(), u + " liked your post"); return added;
    }
    public boolean unlikePost(String user, long id) { return likes.containsKey(id) && likes.get(id).remove(normalize(user)); }
    public boolean comment(String user, long id, String text) {
        String u = normalize(user); Post p = posts.get(id);
        if (p == null || !users.containsKey(u) || !canView(u, p) || blank(text) || text.length() > 250) return false;
        comments.get(id).add(u + ": " + text.trim()); if (!u.equals(p.author())) notify(p.author(), u + " commented on your post"); return true;
    }

    public boolean sendMessage(String from, String to, String text) {
        String a = normalize(from), b = normalize(to);
        if (!validPair(a, b) || blank(text) || text.length() > 1000 || blocked.get(a).contains(b) || blocked.get(b).contains(a)) return false;
        messages.add(new Message(a, b, text.trim(), LocalDateTime.now())); notify(b, "New message from " + a); return true;
    }
    public List<String> searchUsers(String query) {
        if (blank(query)) return List.of(); String q = query.trim().toLowerCase();
        return users.values().stream().filter(User::active).filter(u -> u.username().contains(q) || u.displayName().toLowerCase().contains(q)).map(User::username).toList();
    }
    public Optional<User> user(String username) { return Optional.ofNullable(users.get(normalize(username))); }
    public Optional<Post> post(long id) { return Optional.ofNullable(posts.get(id)); }
    public int friendCount(String username) { return friends.getOrDefault(normalize(username), Set.of()).size(); }
    public int likeCount(long id) { return likes.getOrDefault(id, Set.of()).size(); }
    public int commentCount(long id) { return comments.getOrDefault(id, List.of()).size(); }
    public int messageCount() { return messages.size(); }
    public List<String> notifications(String username) { return List.copyOf(notifications.getOrDefault(normalize(username), List.of())); }

    private boolean canView(String viewer, Post post) { return post.privacy() == Privacy.PUBLIC || viewer.equals(post.author()) || (post.privacy() == Privacy.FRIENDS && friends.get(post.author()).contains(viewer)); }
    private boolean validPair(String a, String b) { return a != null && b != null && !a.equals(b) && users.containsKey(a) && users.containsKey(b); }
    private boolean emailTaken(String email) { return users.values().stream().anyMatch(u -> u.email().equalsIgnoreCase(email)); }
    private void notify(String user, String note) { notifications.get(user).add(note); }
    private static String normalize(String value) { return value == null ? null : value.trim().toLowerCase(); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static boolean validUsername(String value) { return value != null && value.matches("[A-Za-z0-9_]{3,20}"); }
    private static boolean validEmail(String value) { return value != null && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); }
    private static boolean validPassword(String value) { return value != null && value.length() >= 8 && value.matches(".*[A-Za-z].*") && value.matches(".*\\d.*"); }
}
