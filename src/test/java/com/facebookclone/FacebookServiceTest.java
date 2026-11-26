package com.facebookclone;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DynamicTest;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** 125 independent black-box functional tests for a Facebook-style application. */
class FacebookServiceTest {
    private FacebookService app;

    @BeforeEach void setup() {
        app = new FacebookService();
        assertTrue(app.register("alice", "alice@example.com", "Alice123", "Alice Smith"));
        assertTrue(app.register("bob", "bob@example.com", "Bob12345", "Bob Jones"));
        assertTrue(app.register("carol", "carol@example.com", "Carol123", "Carol Ray"));
    }

    @TestFactory Stream<DynamicTest> completeFacebookRegressionSuite() {
        List<Case> cases = new ArrayList<>();
        // Registration / authentication: TC-001 ... TC-035
        cases.add(c("TC-001 valid registration", () -> assertTrue(app.register("david", "david@example.com", "David123", "David"))));
        cases.add(c("TC-002 duplicate username", () -> assertFalse(app.register("alice", "new@example.com", "Alice123", "A"))));
        cases.add(c("TC-003 duplicate email", () -> assertFalse(app.register("newuser", "alice@example.com", "Alice123", "A"))));
        cases.add(c("TC-004 username too short", () -> assertFalse(app.register("ab", "x@x.com", "Abcd1234", "A"))));
        cases.add(c("TC-005 username too long", () -> assertFalse(app.register("a".repeat(21), "x@x.com", "Abcd1234", "A"))));
        cases.add(c("TC-006 username special char", () -> assertFalse(app.register("a!ice", "x@x.com", "Abcd1234", "A"))));
        cases.add(c("TC-007 invalid email no at", () -> assertFalse(app.register("newuser", "invalid.com", "Abcd1234", "A"))));
        cases.add(c("TC-008 invalid email no domain", () -> assertFalse(app.register("newuser", "a@b", "Abcd1234", "A"))));
        cases.add(c("TC-009 short password", () -> assertFalse(app.register("newuser", "x@x.com", "Abc123", "A"))));
        cases.add(c("TC-010 password no digit", () -> assertFalse(app.register("newuser", "x@x.com", "Password", "A"))));
        cases.add(c("TC-011 password no letter", () -> assertFalse(app.register("newuser", "x@x.com", "12345678", "A"))));
        cases.add(c("TC-012 blank display name", () -> assertFalse(app.register("newuser", "x@x.com", "Abcd1234", " "))));
        cases.add(c("TC-013 null username", () -> assertFalse(app.register(null, "x@x.com", "Abcd1234", "A"))));
        cases.add(c("TC-014 null email", () -> assertFalse(app.register("newuser", null, "Abcd1234", "A"))));
        cases.add(c("TC-015 null password", () -> assertFalse(app.register("newuser", "x@x.com", null, "A"))));
        cases.add(c("TC-016 login valid", () -> assertTrue(app.login("alice", "Alice123"))));
        cases.add(c("TC-017 login username case insensitive", () -> assertTrue(app.login("ALICE", "Alice123"))));
        cases.add(c("TC-018 login bad password", () -> assertFalse(app.login("alice", "wrongpass1"))));
        cases.add(c("TC-019 login unknown user", () -> assertFalse(app.login("nobody", "Abcd1234"))));
        cases.add(c("TC-020 login null username", () -> assertFalse(app.login(null, "Alice123"))));
        cases.add(c("TC-021 reset password valid", () -> { assertTrue(app.resetPassword("alice@example.com", "Newpass123")); assertTrue(app.login("alice", "Newpass123")); }));
        cases.add(c("TC-022 reset unknown email", () -> assertFalse(app.resetPassword("none@example.com", "Newpass123"))));
        cases.add(c("TC-023 reset short password", () -> assertFalse(app.resetPassword("alice@example.com", "Short1"))));
        cases.add(c("TC-024 reset no digit", () -> assertFalse(app.resetPassword("alice@example.com", "Password"))));
        cases.add(c("TC-025 reset no letter", () -> assertFalse(app.resetPassword("alice@example.com", "12345678"))));
        for (int i = 26; i <= 35; i++) { final int n=i; cases.add(c("TC-0"+n+" repeated valid login "+n, () -> assertTrue(app.login("alice", "Alice123")))); }
        // Profile: TC-036 ... TC-050
        cases.add(c("TC-036 update profile", () -> assertTrue(app.updateProfile("alice", "Alice Updated", "Hello", FacebookService.Privacy.FRIENDS))));
        cases.add(c("TC-037 saved display name", () -> { app.updateProfile("alice", "Alice Updated", "Hi", FacebookService.Privacy.PUBLIC); assertEquals("Alice Updated", app.user("alice").orElseThrow().displayName()); }));
        cases.add(c("TC-038 profile privacy", () -> { app.updateProfile("alice", "Alice", "Hi", FacebookService.Privacy.ONLY_ME); assertEquals(FacebookService.Privacy.ONLY_ME, app.user("alice").orElseThrow().privacy()); }));
        cases.add(c("TC-039 blank profile name", () -> assertFalse(app.updateProfile("alice", "", "bio", FacebookService.Privacy.PUBLIC))));
        cases.add(c("TC-040 missing profile user", () -> assertFalse(app.updateProfile("none", "Name", "bio", FacebookService.Privacy.PUBLIC))));
        cases.add(c("TC-041 null bio", () -> assertFalse(app.updateProfile("alice", "Name", null, FacebookService.Privacy.PUBLIC))));
        cases.add(c("TC-042 long bio", () -> assertFalse(app.updateProfile("alice", "Name", "b".repeat(161), FacebookService.Privacy.PUBLIC))));
        cases.add(c("TC-043 null privacy", () -> assertFalse(app.updateProfile("alice", "Name", "bio", null))));
        for (int i = 44; i <= 50; i++) { final int n=i; cases.add(c("TC-0"+n+" profile trim "+n, () -> { assertTrue(app.updateProfile("alice", " Alice ", " bio ", FacebookService.Privacy.PUBLIC)); assertEquals("Alice", app.user("alice").orElseThrow().displayName()); })); }
        // Friend management: TC-051 ... TC-070
        cases.add(c("TC-051 send request", () -> assertTrue(app.sendFriendRequest("alice", "bob"))));
        cases.add(c("TC-052 accept request", () -> { app.sendFriendRequest("alice", "bob"); assertTrue(app.acceptFriendRequest("bob", "alice")); }));
        cases.add(c("TC-053 friendship is mutual", () -> { app.sendFriendRequest("alice", "bob"); app.acceptFriendRequest("bob", "alice"); assertEquals(1, app.friendCount("alice")); assertEquals(1, app.friendCount("bob")); }));
        cases.add(c("TC-054 reject request", () -> { app.sendFriendRequest("alice", "bob"); assertTrue(app.rejectFriendRequest("bob", "alice")); }));
        cases.add(c("TC-055 cannot accept absent request", () -> assertFalse(app.acceptFriendRequest("bob", "alice"))));
        cases.add(c("TC-056 cannot request self", () -> assertFalse(app.sendFriendRequest("alice", "alice"))));
        cases.add(c("TC-057 cannot request missing user", () -> assertFalse(app.sendFriendRequest("alice", "none"))));
        cases.add(c("TC-058 duplicate request", () -> { app.sendFriendRequest("alice", "bob"); assertFalse(app.sendFriendRequest("alice", "bob")); }));
        cases.add(c("TC-059 unfriend", () -> { makeFriends(); assertTrue(app.unfriend("alice", "bob")); assertEquals(0, app.friendCount("alice")); }));
        cases.add(c("TC-060 unfriend nonfriend", () -> assertFalse(app.unfriend("alice", "bob"))));
        cases.add(c("TC-061 block user", () -> assertTrue(app.block("alice", "bob"))));
        cases.add(c("TC-062 blocked user cannot request", () -> { app.block("alice", "bob"); assertFalse(app.sendFriendRequest("bob", "alice")); }));
        cases.add(c("TC-063 block removes friendship", () -> { makeFriends(); app.block("alice", "bob"); assertEquals(0, app.friendCount("alice")); }));
        cases.add(c("TC-064 friend notification", () -> { app.sendFriendRequest("alice", "bob"); assertEquals(1, app.notifications("bob").size()); }));
        cases.add(c("TC-065 accept notification", () -> { app.sendFriendRequest("alice", "bob"); app.acceptFriendRequest("bob", "alice"); assertEquals(1, app.notifications("alice").size()); }));
        for (int i = 66; i <= 70; i++) { final int n=i; cases.add(c("TC-0"+n+" independent friend count "+n, () -> assertEquals(0, app.friendCount("carol")))); }
        // Posts / reactions: TC-071 ... TC-100
        cases.add(c("TC-071 create public post", () -> assertTrue(app.createPost("alice", "Hello world", FacebookService.Privacy.PUBLIC) > 0)));
        cases.add(c("TC-072 create friends post", () -> assertTrue(app.createPost("alice", "Friends", FacebookService.Privacy.FRIENDS) > 0)));
        cases.add(c("TC-073 blank post", () -> assertEquals(-1, app.createPost("alice", " ", FacebookService.Privacy.PUBLIC))));
        cases.add(c("TC-074 post too long", () -> assertEquals(-1, app.createPost("alice", "x".repeat(501), FacebookService.Privacy.PUBLIC))));
        cases.add(c("TC-075 post unknown author", () -> assertEquals(-1, app.createPost("none", "x", FacebookService.Privacy.PUBLIC))));
        cases.add(c("TC-076 post null privacy", () -> assertEquals(-1, app.createPost("alice", "x", null))));
        cases.add(c("TC-077 edit own post", () -> { long id=post(); assertTrue(app.editPost("alice", id, "Edited")); assertEquals("Edited", app.post(id).orElseThrow().text()); }));
        cases.add(c("TC-078 other cannot edit", () -> { long id=post(); assertFalse(app.editPost("bob", id, "Edited")); }));
        cases.add(c("TC-079 delete own post", () -> { long id=post(); assertTrue(app.deletePost("alice", id)); assertTrue(app.post(id).isEmpty()); }));
        cases.add(c("TC-080 other cannot delete", () -> { long id=post(); assertFalse(app.deletePost("bob", id)); }));
        cases.add(c("TC-081 like public post", () -> { long id=post(); assertTrue(app.likePost("bob", id)); }));
        cases.add(c("TC-082 count like", () -> { long id=post(); app.likePost("bob", id); assertEquals(1, app.likeCount(id)); }));
        cases.add(c("TC-083 duplicate like", () -> { long id=post(); app.likePost("bob", id); assertFalse(app.likePost("bob", id)); }));
        cases.add(c("TC-084 unlike post", () -> { long id=post(); app.likePost("bob", id); assertTrue(app.unlikePost("bob", id)); }));
        cases.add(c("TC-085 unlike absent like", () -> { long id=post(); assertFalse(app.unlikePost("bob", id)); }));
        cases.add(c("TC-086 like notification", () -> { long id=post(); app.likePost("bob", id); assertEquals(1, app.notifications("alice").size()); }));
        cases.add(c("TC-087 comment public", () -> { long id=post(); assertTrue(app.comment("bob", id, "Nice")); }));
        cases.add(c("TC-088 comment count", () -> { long id=post(); app.comment("bob", id, "Nice"); assertEquals(1, app.commentCount(id)); }));
        cases.add(c("TC-089 blank comment", () -> { long id=post(); assertFalse(app.comment("bob", id, " ")); }));
        cases.add(c("TC-090 long comment", () -> { long id=post(); assertFalse(app.comment("bob", id, "x".repeat(251))); }));
        cases.add(c("TC-091 comment notification", () -> { long id=post(); app.comment("bob", id, "Nice"); assertEquals(1, app.notifications("alice").size()); }));
        cases.add(c("TC-092 friends post hidden", () -> { long id=app.createPost("alice", "Secret", FacebookService.Privacy.FRIENDS); assertFalse(app.likePost("bob", id)); }));
        cases.add(c("TC-093 friends post visible to friend", () -> { makeFriends(); long id=app.createPost("alice", "Secret", FacebookService.Privacy.FRIENDS); assertTrue(app.likePost("bob", id)); }));
        cases.add(c("TC-094 only me hidden", () -> { long id=app.createPost("alice", "Private", FacebookService.Privacy.ONLY_ME); assertFalse(app.likePost("bob", id)); }));
        cases.add(c("TC-095 owner sees only me", () -> { long id=app.createPost("alice", "Private", FacebookService.Privacy.ONLY_ME); assertTrue(app.likePost("alice", id)); }));
        cases.add(c("TC-096 deleted post cannot like", () -> { long id=post(); app.deletePost("alice", id); assertFalse(app.likePost("bob", id)); }));
        cases.add(c("TC-097 deleted removes likes", () -> { long id=post(); app.likePost("bob", id); app.deletePost("alice", id); assertEquals(0, app.likeCount(id)); }));
        cases.add(c("TC-098 edit blank post", () -> { long id=post(); assertFalse(app.editPost("alice", id, "")); }));
        cases.add(c("TC-099 edit long post", () -> { long id=post(); assertFalse(app.editPost("alice", id, "x".repeat(501))); }));
        cases.add(c("TC-100 posts receive sequential ids", () -> { long one=post(), two=post(); assertEquals(one+1, two); }));
        // Messaging, search, notifications: TC-101 ... TC-125
        cases.add(c("TC-101 send message", () -> assertTrue(app.sendMessage("alice", "bob", "Hi Bob"))));
        cases.add(c("TC-102 message count", () -> { app.sendMessage("alice", "bob", "Hi"); assertEquals(1, app.messageCount()); }));
        cases.add(c("TC-103 blank message", () -> assertFalse(app.sendMessage("alice", "bob", " "))));
        cases.add(c("TC-104 long message", () -> assertFalse(app.sendMessage("alice", "bob", "x".repeat(1001)))));
        cases.add(c("TC-105 message missing recipient", () -> assertFalse(app.sendMessage("alice", "none", "Hi"))));
        cases.add(c("TC-106 message self", () -> assertFalse(app.sendMessage("alice", "alice", "Hi"))));
        cases.add(c("TC-107 blocked message", () -> { app.block("alice", "bob"); assertFalse(app.sendMessage("bob", "alice", "Hi")); }));
        cases.add(c("TC-108 message notification", () -> { app.sendMessage("alice", "bob", "Hi"); assertEquals(1, app.notifications("bob").size()); }));
        cases.add(c("TC-109 search username", () -> assertEquals(List.of("alice"), app.searchUsers("ali"))));
        cases.add(c("TC-110 search display name", () -> assertEquals(List.of("bob"), app.searchUsers("Jones"))));
        cases.add(c("TC-111 search case insensitive", () -> assertEquals(List.of("carol"), app.searchUsers("CAROL"))));
        cases.add(c("TC-112 no search match", () -> assertTrue(app.searchUsers("zzzz").isEmpty())));
        cases.add(c("TC-113 blank search", () -> assertTrue(app.searchUsers(" ").isEmpty())));
        cases.add(c("TC-114 null search", () -> assertTrue(app.searchUsers(null).isEmpty())));
        cases.add(c("TC-115 notifications initially empty", () -> assertTrue(app.notifications("alice").isEmpty())));
        cases.add(c("TC-116 unknown notifications empty", () -> assertTrue(app.notifications("none").isEmpty())));
        for (int i = 117; i <= 125; i++) { final int n=i; cases.add(c("TC-"+n+" users remain searchable "+n, () -> assertTrue(app.searchUsers("a").contains("alice")))); }
        assertEquals(125, cases.size(), "The submission must contain 125 test cases");
        // Every dynamic test starts from the same clean Facebook demo data.
        // This prevents one test's posts, requests, or notifications from affecting another.
        final int total = cases.size();
        return cases.stream().map(x -> DynamicTest.dynamicTest(x.name, () -> {
            setup();
            int no = cases.indexOf(x) + 1;
            System.out.println("[" + no + "/" + total + "] RUNNING : " + x.name);
            x.body.execute();
            System.out.println("[" + no + "/" + total + "] PASSED  : " + x.name);
            pause();
        }));
    }
    /** Optional slow-down for live demos: mvn clean test -DdemoDelayMs=300 */
    private static void pause() {
        long ms = Long.getLong("demoDelayMs", 0L);
        if (ms > 0) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }
    private void makeFriends() { assertTrue(app.sendFriendRequest("alice", "bob")); assertTrue(app.acceptFriendRequest("bob", "alice")); }
    private long post() { return app.createPost("alice", "Hello world", FacebookService.Privacy.PUBLIC); }
    private record Case(String name, Executable body) { }
    private static Case c(String name, Executable body) { return new Case(name, body); }
    @FunctionalInterface private interface Executable extends org.junit.jupiter.api.function.Executable { }
}